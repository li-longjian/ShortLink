package org.llj.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import groovy.util.logging.Slf4j;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jodd.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.llj.shortlink.project.common.Exception.ClientException;
import org.llj.shortlink.project.common.Exception.ServiceException;
import org.llj.shortlink.project.dao.entity.*;
import org.llj.shortlink.project.dao.mapper.*;
import org.llj.shortlink.project.dto.req.LinkCreateReqDTO;
import org.llj.shortlink.project.dto.req.LinkUpdateReqDTO;
import org.llj.shortlink.project.dto.req.ShortLinkPageReqDTO;
import org.llj.shortlink.project.dto.resp.GroupLinkCountRespDTO;
import org.llj.shortlink.project.dto.resp.LinkCreateRespDTO;
import org.llj.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import org.llj.shortlink.project.service.ShortLinkService;
import org.llj.shortlink.project.utils.HashUtil;
import org.llj.shortlink.project.utils.LinkUtil;
import org.llj.shortlink.project.utils.SetCacheTimeUtil;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.server.ServerCloneException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.llj.shortlink.project.common.constant.LinkConstant.AMP_URL;
import static org.llj.shortlink.project.common.constant.RedisKey.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {
    private  final RBloomFilter<String> rBloomFilter;
    private final ShortLinkGotoMapper gotoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final ShortLinkStatsMapper shortLinkStatsMapper;
    private final LinkLocateStatsMapper linkLocateStatsMapper;
    private final LinkOSStatsMapper linkOSStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;


    private  final String AMAP_KEY = "c1ce6eed90ea948651c4ad0ae6793cdc";
    /**
     * 短连接跳转源链接
     * @param shortUri
     * @param request
     * @param response
     */
    @SneakyThrows
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void reStoreUrl(String shortUri, HttpServletRequest request, HttpServletResponse response) {
        String serverName = request.getServerName();
        String scheme = request.getScheme();
        String fullShortUrl = serverName + "/"  + shortUri;
        String originalLink = stringRedisTemplate.opsForValue().get(String.format(FULL_SHORT_URL_KEY, fullShortUrl));
        if(StringUtil.isNotBlank(originalLink)) {
            shortLinkStats(fullShortUrl,null,request,response);
            response.sendRedirect(originalLink);
            return;

        }
        boolean contains = rBloomFilter.contains(fullShortUrl); //防止缓存穿透
        if(!contains) {
            response.sendRedirect("/page/notfound");
            return;
        }
        String goToIsNull = stringRedisTemplate.opsForValue().get(String.format(IS_NULL_FULL_SHORT_URL_KEY, fullShortUrl));
        if(StringUtil.isNotBlank(goToIsNull)) {
            response.sendRedirect("/page/notfound");
            return;
        }

        RLock lock = redissonClient.getLock(String.format(LOCK_FULL_SHORT_URL_KEY, fullShortUrl)); //加分布式锁,防止缓存击穿
        lock.lock();
        try{
           originalLink = stringRedisTemplate.opsForValue().get(String.format(FULL_SHORT_URL_KEY, fullShortUrl));
            if(StringUtil.isNotBlank(originalLink)) {
                shortLinkStats(fullShortUrl,null,request,response);
                response.sendRedirect(originalLink);
                return;
            }
            LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
            ShortLinkGotoDO gotoDO = gotoMapper.selectOne(queryWrapper);
            if(gotoDO == null) {
                stringRedisTemplate.opsForValue().set(String.format(IS_NULL_FULL_SHORT_URL_KEY, fullShortUrl),"-");//如果数据库中不存在，为防止缓存穿透，设置一个特殊值
                response.sendRedirect("/page/notfound");
                return ;
            }
            LambdaQueryWrapper<ShortLinkDO> wrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, gotoDO.getGid())
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl)
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .eq(ShortLinkDO::getDelFlag, 0);
            ShortLinkDO linkDO = baseMapper.selectOne(wrapper);

            if(linkDO == null || (linkDO.getValidDate() != null && linkDO.getValidDate().isBefore(LocalDateTime.now()))){//短连接已经过期
                stringRedisTemplate.opsForValue().set(String.format(IS_NULL_FULL_SHORT_URL_KEY, fullShortUrl),"-");
                response.sendRedirect("/page/notfound");
                return ;
            }
            long validTime = SetCacheTimeUtil.getLinkCacheExpirationSeconds(linkDO.getValidDate());
            stringRedisTemplate.opsForValue().set(
                    String.format(FULL_SHORT_URL_KEY, fullShortUrl),
                    linkDO.getOriginUrl(),
                    validTime,
                    TimeUnit.SECONDS
            );
            shortLinkStats(fullShortUrl,linkDO.getGid(),request,response);
            response.sendRedirect(linkDO.getOriginUrl());
        }finally {
            lock.unlock();
        }

    }


    /**
     * 创建短连接
     * @param linkCreateReqDTO
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public LinkCreateRespDTO createShortLink(LinkCreateReqDTO linkCreateReqDTO) {
        String shortLinkSuffix = generateLinkSuffix(linkCreateReqDTO);
        String fullShortLinkUrl = linkCreateReqDTO.getDomain() + '/' + shortLinkSuffix;
        LambdaQueryWrapper<ShortLinkDO> QueryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, fullShortLinkUrl);
        ShortLinkDO linkDO = baseMapper.selectOne(QueryWrapper);
        if(linkDO != null) throw new ServiceException("当前短连接已存在");
        rBloomFilter.add(fullShortLinkUrl);
        ShortLinkDO shortLinkDO =  ShortLinkDO.builder()
                .domain(linkCreateReqDTO.getDomain())
                .shortUri(shortLinkSuffix)
                .originUrl(linkCreateReqDTO.getOriginUrl())
                .fullShortUrl(fullShortLinkUrl)
                .gid(linkCreateReqDTO.getGid())
                .describe(linkCreateReqDTO.getDescribe())
                .createdType(linkCreateReqDTO.getCreatedType())
                .validDateType(linkCreateReqDTO.getValidDateType())
                .validDate(linkCreateReqDTO.getValidDate())
                .clickNum(0)
                .favicon(getFavicon(linkCreateReqDTO.getOriginUrl()))
                .build();
        ShortLinkGotoDO gotoDO = ShortLinkGotoDO.builder()
                .gid(linkCreateReqDTO.getGid())
                .fullShortUrl(fullShortLinkUrl)
                .build();
        try{
            baseMapper.insert(shortLinkDO);
            gotoMapper.insert(gotoDO);
        }catch (DuplicateKeyException exception){
            log.warn("短链接:"+fullShortLinkUrl+" 重复入库");
            throw  new ServiceException("短链接重复");
        }
        long validTime = SetCacheTimeUtil.getLinkCacheExpirationSeconds(linkCreateReqDTO.getValidDate());
        stringRedisTemplate.opsForValue().set(
                String.format(FULL_SHORT_URL_KEY, fullShortLinkUrl),
                shortLinkDO.getOriginUrl(),
                validTime,
                TimeUnit.SECONDS
                );
        return LinkCreateRespDTO.builder()
                .fullShortUrl(shortLinkDO.getFullShortUrl())
                .originUrl(shortLinkDO.getOriginUrl())
                .gid(shortLinkDO.getGid())
                .clickNum(shortLinkDO.getClickNum())
                .build();
    }

    @Override
    public IPage<ShortLinkPageRespDTO> getPage(ShortLinkPageReqDTO shortLinkPageReqDTO) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, shortLinkPageReqDTO.getGid())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0)
                .orderByDesc(ShortLinkDO::getCreateTime);
        IPage<ShortLinkDO> pageResults = baseMapper.selectPage(shortLinkPageReqDTO, queryWrapper);
        return pageResults.convert(res -> BeanUtil.toBean(res, ShortLinkPageRespDTO.class));
    }

    @Override
    public List<GroupLinkCountRespDTO> getGroupLinkCount(List<String> requestParam) {

        QueryWrapper<ShortLinkDO> queryWrapper = Wrappers.query(ShortLinkDO.class)
                .select("gid as gid, count(*) as shortLinkCount")
                .in("gid", requestParam)
                .eq("enable_status", 0)
                .groupBy("gid");
        List<Map<String, Object>> Links = baseMapper.selectMaps(queryWrapper);
        return BeanUtil.copyToList(Links, GroupLinkCountRespDTO.class);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void updateLink(LinkUpdateReqDTO linkUpdateReqDTO) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, linkUpdateReqDTO.getGid())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0);
        ShortLinkDO linkDO = baseMapper.selectOne(queryWrapper);
        if(linkDO == null) throw  new ClientException("不存在当前短链接");
        ShortLinkDO newLink = ShortLinkDO.builder()
                .shortUri(linkDO.getShortUri())
                .domain(linkDO.getDomain())
                .clickNum(linkDO.getClickNum())
                .favicon(linkDO.getFavicon())
                .createdType(linkDO.getCreatedType())
                .gid(linkUpdateReqDTO.getGid())
                .originUrl(linkUpdateReqDTO.getOriginUrl())
                .fullShortUrl(linkUpdateReqDTO.getFullShortUrl())
                .validDateType(linkUpdateReqDTO.getValidDateType())
                .validDate(linkUpdateReqDTO.getValidDate())
                .describe(linkUpdateReqDTO.getDescribe())
                .build();
        if(Objects.equals(linkUpdateReqDTO.getGid(),linkUpdateReqDTO.getNewGid())){
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, linkUpdateReqDTO.getGid())
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getFullShortUrl, linkDO.getFullShortUrl())
                    .set(Objects.equals(linkUpdateReqDTO.getValidDateType(), 0), ShortLinkDO::getValidDate, null);
            baseMapper.update(newLink,updateWrapper);

        }else {
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, linkUpdateReqDTO.getGid())
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getFullShortUrl,linkDO.getFullShortUrl());
            baseMapper.delete(updateWrapper);
            newLink.setGid(linkUpdateReqDTO.getNewGid());
            baseMapper.update(newLink,updateWrapper);
        }

    }

    public  String generateLinkSuffix(LinkCreateReqDTO linkCreateReqDTO){
        int cnt = 0;
        int maxTryCount = 10;
        String originUrl = linkCreateReqDTO.getOriginUrl();
        String shortLinkSuffix = null;
        while (true) {
            if(cnt >= maxTryCount) try {
                throw  new ServerCloneException("操作频繁，请稍后再试");
            } catch (ServerCloneException e) {
                throw new RuntimeException(e);
            }
            originUrl += System.currentTimeMillis();
            shortLinkSuffix = HashUtil.hashToBase62(originUrl);
            String fullShortLinkUrl = linkCreateReqDTO.getDomain() + '/' + shortLinkSuffix;
            if(!rBloomFilter.contains(fullShortLinkUrl)){
                break;
            }
            cnt++;
        }

        return shortLinkSuffix;
    }

    /**
     * 获取原链接网站图标
     * @param url
     * @return
     */
    @SneakyThrows
    private String getFavicon(String url) {
        URL targetUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("GET");
        connection.connect();
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            String redirectUrl = connection.getHeaderField("Location");
            if (redirectUrl != null) {
                URL newUrl = new URL(redirectUrl);
                connection = (HttpURLConnection) newUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                responseCode = connection.getResponseCode();
            }
        }
        if (responseCode == HttpURLConnection.HTTP_OK) {
            Document document = Jsoup.connect(url).get();
            Element faviconLink = document.select("link[rel~=(?i)^(shortcut )?icon]").first();
            if (faviconLink != null) {
                return faviconLink.attr("abs:href");
            }
        }
        return null;
    }


    @Transactional(propagation = Propagation.REQUIRED)
    protected void shortLinkStats(String fullShortUrl, String gid, HttpServletRequest request, HttpServletResponse response){
        AtomicBoolean uvFirstFlag = new AtomicBoolean();
        Cookie[] cookies = request.getCookies();


        // 构建Redis键名（保留页面路径信息）
        String redisUvKey = REDIS_UV_KEY + fullShortUrl;
        String redisUipKey = REDIS_UIP_KEY +fullShortUrl;
        Runnable addCookieTask = () ->{
            String uv = UUID.randomUUID().toString();
            Cookie cookie = new Cookie("uv",uv);
            cookie.setPath(StrUtil.sub(fullShortUrl,fullShortUrl.indexOf('/'),fullShortUrl.length()));
            cookie.setMaxAge(30 * 24 * 60 * 60);
            response.addCookie(cookie);
            uvFirstFlag.set(true);
            stringRedisTemplate.opsForSet().add(redisUvKey, uv);
        };
        try {
            if(ArrayUtil.isNotEmpty(cookies)){
                Arrays.stream(cookies)
                        .filter(each -> Objects.equals(each.getName(),"uv"))
                        .findFirst()
                        .map(Cookie::getValue)
                        .ifPresentOrElse(each ->{
                            Long UVAdd = stringRedisTemplate.opsForSet().add(redisUvKey, each);
                            uvFirstFlag.set(UVAdd != null && UVAdd > 0); //true : 第一次访问这个页面, false : 不是第一次访问页面

                        }, addCookieTask);
            }else{
                addCookieTask.run();
            }
            /**
             * 获取请求的ip地址,统计uip
             */
            String ip = LinkUtil.getIP(request);
            Long UIPAdd = stringRedisTemplate.opsForSet().add(redisUipKey,ip);
            boolean uipFirstFlag = UIPAdd != null && UIPAdd > 0;

            if(StrUtil.isBlank(gid)){
                LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
                ShortLinkGotoDO gotoDO = gotoMapper.selectOne(queryWrapper);
                gid = gotoDO.getGid();
            }
            /**
             * 获取时间日期等信息
             */
            Date now = new Date();
            int hour = DateUtil.hour(now,true);
            Week week = DateUtil.dayOfWeekEnum(now);
            int weekValue = week.getIso8601Value();
            ShortLinkStatsDO statsDO = ShortLinkStatsDO.builder()
                    .gid(gid)
                    .fullShortUrl(fullShortUrl)
                    .pv(1)
                    .uv(uvFirstFlag.get() ? 1 : 0) //uvFlag true: 第一次访问+1， 否则为0
                    .uip(uipFirstFlag ? 1 : 0)
                    .hour(hour)
                    .weekday(weekValue)
                    .date(now)
                    .build();

            //调用高德地图API获取定位信息
            Map<String, Object> apiRequestParam = new HashMap<>();
            apiRequestParam.put("ip",ip);
            apiRequestParam.put("key",AMAP_KEY);
            String result = HttpUtil.get(AMP_URL, apiRequestParam);
            JSONObject locateObject = JSON.parseObject(result);
            String infocode = locateObject.getString("infocode");
            String province = locateObject.getString("province");
            String city = locateObject.getString("city");
            String adcode = locateObject.getString("adcode");
            if(StrUtil.isBlank(infocode) || !StrUtil.equals("10000",infocode)) {
                province = "未知地区";
                city = "未知地区";
                adcode = "未知";
            }
            LinkLocateStatsDO locateStatsDO = LinkLocateStatsDO.builder()
                    .cnt(1)
                    .country("中国")
                    .gid(gid)
                    .adcode(adcode)
                    .province(province)
                    .city(city)
                    .date(new Date())
                    .fullShortUrl(fullShortUrl)
                    .build();
            shortLinkStatsMapper.shortLinkStats(statsDO);//更新uv，PV，uip
            linkLocateStatsMapper.add(locateStatsDO);//更新省份，地区
            /**
             * 更新操作系统
             */
            LinkOSStatsDO osStatsDO = LinkOSStatsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .cnt(1)
                    .date(new Date())
                    .os(LinkUtil.getOs(request))
                    .build();
            linkOSStatsMapper.LinkOsStats(osStatsDO);

            /**
             * 更新浏览器信息
             */
            LinkBrowserStatsDO browserStatsDO = LinkBrowserStatsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .cnt(1)
                    .date(new Date())
                    .browser(LinkUtil.getBrowser(request))
                    .build();
            linkBrowserStatsMapper.LinkBrowserStats(browserStatsDO);
        } catch (Exception e){
           throw new RuntimeException(e);
        }

    }
}
