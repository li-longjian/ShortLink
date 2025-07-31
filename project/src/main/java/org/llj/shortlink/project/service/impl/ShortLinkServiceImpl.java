package org.llj.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
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
import org.llj.shortlink.project.common.config.GoToDomainWhiteListConfiguration;
import org.llj.shortlink.project.dao.entity.*;
import org.llj.shortlink.project.dao.mapper.*;
import org.llj.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import org.llj.shortlink.project.dto.req.LinkCreateReqDTO;
import org.llj.shortlink.project.dto.req.LinkUpdateReqDTO;
import org.llj.shortlink.project.dto.req.ShortLinkBatchCreateReqDTO;
import org.llj.shortlink.project.dto.req.ShortLinkPageReqDTO;
import org.llj.shortlink.project.dto.resp.*;
import org.llj.shortlink.project.mq.producer.DelayShortLinkStatsMQProducer;
import org.llj.shortlink.project.mq.producer.DelayShortLinkStatsProducer;
import org.llj.shortlink.project.mq.producer.ShortLinkStatsMQProducer;
import org.llj.shortlink.project.service.LinkStatsTodayService;
import org.llj.shortlink.project.service.ShortLinkService;
import org.llj.shortlink.project.utils.HashUtil;
import org.llj.shortlink.project.utils.LinkUtil;
import org.llj.shortlink.project.utils.SetCacheTimeUtil;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.llj.shortlink.project.common.constant.LinkConstant.AMP_URL;
import static org.llj.shortlink.project.common.constant.RedisKey.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {
    private final RBloomFilter<String> rBloomFilter;
    private final ShortLinkGotoMapper gotoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final ShortLinkStatsMapper shortLinkStatsMapper;
    private final LinkLocateStatsMapper linkLocateStatsMapper;
    private final LinkOSStatsMapper linkOSStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final LinkTodayStatsMapper linkTodayStatsMapper;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final LinkStatsTodayService linkStatsTodayService;
    private final DelayShortLinkStatsProducer delayShortLinkStatsProducer; //redisson 实现的延时队列
    private final DelayShortLinkStatsMQProducer delayShortLinkStatsMQProducer; // Rocket Mq 实现的延时队列
    private final ShortLinkStatsMQProducer shortLinkStatsMQProducer; //消息队列 生产者
    private final GoToDomainWhiteListConfiguration goToDomainWhiteListConfiguration;
    private final String AMAP_KEY = "c1ce6eed90ea948651c4ad0ae6793cdc";
    private final String defaultDomain = "nurl.link:8001";

    /**
     * 短连接跳转源链接
     *
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
        String serverPort = Optional.of(request.getServerPort())
                .filter(each -> each != 80)
                .map(String::valueOf)
                .map(each -> ":" + each)
                .orElse("");

        String fullShortUrl = serverName + serverPort + "/" + shortUri;
        String originalLink = stringRedisTemplate.opsForValue().get(String.format(FULL_SHORT_URL_KEY, fullShortUrl));
        if (StringUtil.isNotBlank(originalLink)) {
            //shortLinkStats(fullShortUrl, null, request, response);

            response.sendRedirect(originalLink);//先给用户返回跳转
            //在消息队列 进行数据统计
            ShortLinkStatsRecordDTO statsRecordDTO = builderLinkStatsRecordAndSetUser(fullShortUrl, request, response);
            shortLinkStatsMQProducer.sendMessage(statsRecordDTO);
            return;

        }
        boolean contains = rBloomFilter.contains(fullShortUrl); //防止缓存穿透
        if (!contains) {
            response.sendRedirect("/page/notfound");
            return;
        }
        String goToIsNull = stringRedisTemplate.opsForValue().get(String.format(IS_NULL_FULL_SHORT_URL_KEY, fullShortUrl));
        if (StringUtil.isNotBlank(goToIsNull)) {
            response.sendRedirect("/page/notfound");
            return;
        }

        RLock lock = redissonClient.getLock(String.format(LOCK_FULL_SHORT_URL_KEY, fullShortUrl)); //加分布式锁,防止缓存击穿
        lock.lock();
        try {
            originalLink = stringRedisTemplate.opsForValue().get(String.format(FULL_SHORT_URL_KEY, fullShortUrl));
            if (StringUtil.isNotBlank(originalLink)) {

                response.sendRedirect(originalLink);
                ShortLinkStatsRecordDTO statsRecordDTO = builderLinkStatsRecordAndSetUser(fullShortUrl, request, response);
                shortLinkStatsMQProducer.sendMessage(statsRecordDTO);
                return;
            }
            //优化大量空缓存访问数据库
            String gotoIsNULL = stringRedisTemplate.opsForValue().get(String.format(IS_NULL_FULL_SHORT_URL_KEY, fullShortUrl));
            if (StringUtil.isNotBlank(gotoIsNULL)) {
                response.sendRedirect("/page/notfound");
                return;
            }
            LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
            ShortLinkGotoDO gotoDO = gotoMapper.selectOne(queryWrapper);
            if (gotoDO == null) {
                stringRedisTemplate.opsForValue().set(String.format(IS_NULL_FULL_SHORT_URL_KEY, fullShortUrl), "-");//如果数据库中不存在，为防止缓存穿透，设置一个特殊值
                response.sendRedirect("/page/notfound");
                return;
            }
            LambdaQueryWrapper<ShortLinkDO> wrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, gotoDO.getGid())
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl)
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .eq(ShortLinkDO::getDelFlag, 0);
            ShortLinkDO linkDO = baseMapper.selectOne(wrapper);

            if (linkDO == null || (linkDO.getValidDate() != null && linkDO.getValidDate().isBefore(LocalDateTime.now()))) {//短连接已经过期
                stringRedisTemplate.opsForValue().set(String.format(IS_NULL_FULL_SHORT_URL_KEY, fullShortUrl), "-");
                response.sendRedirect("/page/notfound");
                return;
            }
            long validTime = SetCacheTimeUtil.getLinkCacheExpirationSeconds(linkDO.getValidDate());
            stringRedisTemplate.opsForValue().set(
                    String.format(FULL_SHORT_URL_KEY, fullShortUrl),
                    linkDO.getOriginUrl(),
                    validTime,
                    TimeUnit.SECONDS
            );

            response.sendRedirect(linkDO.getOriginUrl());
            ShortLinkStatsRecordDTO statsRecordDTO = builderLinkStatsRecordAndSetUser(fullShortUrl, request, response);
            shortLinkStatsMQProducer.sendMessage(statsRecordDTO);
        } finally {
            lock.unlock();
        }

    }

    /**
     * 批量创建短链接
     *
     * @param requestParam 批量创建短链接请求参数
     * @return
     */
    @Override
    public ShortLinkBatchCreateRespDTO batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam) {
        List<String> originUrls = requestParam.getOriginUrls();
        List<String> describes = requestParam.getDescribes();
        List<ShortLinkBaseInfoRespDTO> result = new ArrayList<>();
        for (int i = 0; i < originUrls.size(); i++) {
            LinkCreateReqDTO shortLinkCreateReqDTO = BeanUtil.toBean(requestParam, LinkCreateReqDTO.class);
            shortLinkCreateReqDTO.setOriginUrl(originUrls.get(i));
            shortLinkCreateReqDTO.setDescribe(describes.get(i));
            try {
                LinkCreateRespDTO shortLink = createShortLink(shortLinkCreateReqDTO);
                ShortLinkBaseInfoRespDTO linkBaseInfoRespDTO = ShortLinkBaseInfoRespDTO.builder()
                        .fullShortUrl(shortLink.getFullShortUrl())
                        .originUrl(shortLink.getOriginUrl())
                        .describe(describes.get(i))
                        .build();
                result.add(linkBaseInfoRespDTO);
            } catch (Throwable ex) {
                log.error("批量创建短链接失败，原始参数：" + originUrls.get(i));
            }
        }
        return ShortLinkBatchCreateRespDTO.builder()
                .total(result.size())
                .baseLinkInfos(result)
                .build();
    }

    private void verificationWhitelist(String originUrl) {
        Boolean enable = goToDomainWhiteListConfiguration.getEnable();
        if(enable == null || !enable) return;
        String domain = LinkUtil.extractDomain(originUrl);
        if(StrUtil.isBlank(domain)) throw  new ClientException("跳转链接填写错误");
        List<String> details = goToDomainWhiteListConfiguration.getDetails();
        if(!details.contains(domain)){
            throw new ClientException("演示环境为避免恶意攻击，请生成以下网站跳转链接：" + goToDomainWhiteListConfiguration.getNames());
        }
    }

    /**
     * 创建短连接
     *
     * @param linkCreateReqDTO
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public LinkCreateRespDTO createShortLink(LinkCreateReqDTO linkCreateReqDTO) {
        verificationWhitelist(linkCreateReqDTO.getOriginUrl());
        String shortLinkSuffix = generateLinkSuffix(linkCreateReqDTO);
        String fullShortLinkUrl = defaultDomain + '/' + shortLinkSuffix;
        LambdaQueryWrapper<ShortLinkDO> QueryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, fullShortLinkUrl);
        ShortLinkDO linkDO = baseMapper.selectOne(QueryWrapper);
        if (linkDO != null) throw new ServiceException("当前短连接已存在");
        rBloomFilter.add(fullShortLinkUrl);
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(defaultDomain)
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
                .totalPv(0)
                .totalUv(0)
                .totalUip(0)
                .delTime(0L)
                .build();
        ShortLinkGotoDO gotoDO = ShortLinkGotoDO.builder()
                .gid(linkCreateReqDTO.getGid())
                .fullShortUrl(fullShortLinkUrl)
                .build();
        try {
            baseMapper.insert(shortLinkDO);
            gotoMapper.insert(gotoDO);
        } catch (DuplicateKeyException exception) {
            log.warn("短链接:" + fullShortLinkUrl + " 重复入库");
            throw new ServiceException("短链接重复");
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
        IPage<ShortLinkDO> pageResults = baseMapper.pageLink(shortLinkPageReqDTO);
        return pageResults.convert(res -> BeanUtil.toBean(res, ShortLinkPageRespDTO.class));
    }

    @Override
    public List<GroupLinkCountRespDTO> getGroupLinkCount(List<String> requestParam) {

        QueryWrapper<ShortLinkDO> queryWrapper = Wrappers.query(ShortLinkDO.class)
                .select("gid as gid, count(*) as shortLinkCount")
                .in("gid", requestParam)
                .eq("enable_status", 0)
                .eq("del_status", 0)
                .eq("del_time", 0L)
                .groupBy("gid");
        List<Map<String, Object>> Links = baseMapper.selectMaps(queryWrapper);
        return BeanUtil.copyToList(Links, GroupLinkCountRespDTO.class);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void updateLink(LinkUpdateReqDTO linkUpdateReqDTO) {
        verificationWhitelist(linkUpdateReqDTO.getOriginUrl());
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, linkUpdateReqDTO.getGid())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0);
        ShortLinkDO linkDO = baseMapper.selectOne(queryWrapper);
        if (linkDO == null) throw new ClientException("不存在当前短链接");
        ShortLinkDO newLink = ShortLinkDO.builder()
                .shortUri(linkDO.getShortUri())
                .domain(defaultDomain)
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
        if (Objects.equals(linkUpdateReqDTO.getGid(), linkUpdateReqDTO.getNewGid())) {
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, linkUpdateReqDTO.getGid())
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getFullShortUrl, linkDO.getFullShortUrl())
                    .set(Objects.equals(linkUpdateReqDTO.getValidDateType(), 0), ShortLinkDO::getValidDate, null);
            baseMapper.update(newLink, updateWrapper);

        } else {
            /**
             * 当分组发生改变时，即gid会发生变化，短连接表的分片键为gid,因此gid改变，短连接可能映射到其他表中，
             * 同时，此时如果在修改过程中有其他线程访问此短连接的统计数据，则会出现错误结果，因此在这里采用读写锁解决冲突
             * 但是为了使读锁不会一直循环等待，给用户更加良好的体验，因此使用延迟队列
             */
            //定义读写锁
            RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, linkUpdateReqDTO.getFullShortUrl()));
            RLock writeLock = readWriteLock.writeLock();//写锁
            if (!writeLock.tryLock()) { //当前线程尝试获取写锁 ，失败
                throw new ServiceException("当前短连接正在被访问中，请稍后再试");//

            }
            try {//获取了写锁
                LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                        .eq(ShortLinkDO::getGid, linkUpdateReqDTO.getGid())
                        .eq(ShortLinkDO::getEnableStatus, 0)
                        .eq(ShortLinkDO::getDelFlag, 0)
                        .eq(ShortLinkDO::getDelTime, 0L)
                        .eq(ShortLinkDO::getFullShortUrl, linkDO.getFullShortUrl());
                //在原来的link表中删除记录（逻辑删除）
                ShortLinkDO deleteLinkDO = ShortLinkDO.builder().delTime(System.currentTimeMillis()).build();
                deleteLinkDO.setDelFlag(1);
                baseMapper.update(deleteLinkDO, updateWrapper);
                //需要需重插入一条短连接记录（根据新gid进行分片，找到对应的link表
                ShortLinkDO insertShortLinkDO = ShortLinkDO.builder()
                        .shortUri(linkDO.getShortUri())
                        .domain(defaultDomain)
                        .clickNum(linkDO.getClickNum())
                        .favicon(linkDO.getFavicon())
                        .createdType(linkDO.getCreatedType())
                        .gid(linkUpdateReqDTO.getNewGid())
                        .originUrl(linkUpdateReqDTO.getOriginUrl())
                        .fullShortUrl(linkUpdateReqDTO.getFullShortUrl())
                        .validDateType(linkUpdateReqDTO.getValidDateType())
                        .validDate(linkUpdateReqDTO.getValidDate())
                        .describe(linkUpdateReqDTO.getDescribe())
                        .enableStatus(linkDO.getEnableStatus())
                        .totalUip(linkDO.getTotalUip())
                        .totalPv(linkDO.getTotalPv())
                        .totalUv(linkDO.getTotalUv())
                        .delTime(0L)
                        .build();
                baseMapper.insert(insertShortLinkDO);
                //于此同时，stats today表也同样是使用gid作为分片键爱那个，需要先删除后再插入
                LambdaQueryWrapper<LinkTodayStatsDO> wrapper = Wrappers.lambdaQuery(LinkTodayStatsDO.class)
                        .eq(LinkTodayStatsDO::getGid, linkDO.getGid())
                        .eq(LinkTodayStatsDO::getFullShortUrl, linkDO.getFullShortUrl())
                        .eq(LinkTodayStatsDO::getDelFlag, 0);
                List<LinkTodayStatsDO> linkTodayStatsDOList = linkTodayStatsMapper.selectList(wrapper);
                if (CollUtil.isNotEmpty(linkTodayStatsDOList)) {
                    linkTodayStatsMapper.deleteByIds(
                            linkTodayStatsDOList.stream()
                                    .map(LinkTodayStatsDO::getId)
                                    .toList()
                    );//通过id批量删除
                    //批量插入
                    linkTodayStatsDOList.forEach(each -> {
                        each.setGid(linkUpdateReqDTO.getNewGid());//修改为新的分组id
                    });
                    linkStatsTodayService.saveBatch(linkTodayStatsDOList);
                }
                //goto表更新
                LambdaQueryWrapper<ShortLinkGotoDO> linkGotoQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, linkUpdateReqDTO.getFullShortUrl())
                        .eq(ShortLinkGotoDO::getGid, linkDO.getGid());
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoQueryWrapper);
                /**
                 * 此处有疑问： goto表的分片键是 fullShortUrl, 是否需要删除后再插入，还是直接更新gid就行
                 */
                shortLinkGotoMapper.deleteById(shortLinkGotoDO.getId());
                shortLinkGotoDO.setGid(linkUpdateReqDTO.getNewGid());
                shortLinkGotoMapper.insert(shortLinkGotoDO);
                //更新基础统计表
                LambdaUpdateWrapper<ShortLinkStatsDO> linkAccessStatsUpdateWrapper = Wrappers.lambdaUpdate(ShortLinkStatsDO.class)
                        .eq(ShortLinkStatsDO::getFullShortUrl, linkUpdateReqDTO.getFullShortUrl())
                        .eq(ShortLinkStatsDO::getGid, linkUpdateReqDTO.getGid())
                        .eq(ShortLinkStatsDO::getDelFlag, 0);
                ShortLinkStatsDO linkAccessStatsDO = ShortLinkStatsDO.builder()
                        .gid(linkUpdateReqDTO.getNewGid())
                        .build();
                shortLinkStatsMapper.update(linkAccessStatsDO, linkAccessStatsUpdateWrapper);
                //更新地区统计表
                LambdaUpdateWrapper<LinkLocateStatsDO> linkLocaleStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkLocateStatsDO.class)
                        .eq(LinkLocateStatsDO::getFullShortUrl, linkUpdateReqDTO.getFullShortUrl())
                        .eq(LinkLocateStatsDO::getGid, linkDO.getGid())
                        .eq(LinkLocateStatsDO::getDelFlag, 0);
                LinkLocateStatsDO linkLocaleStatsDO = LinkLocateStatsDO.builder()
                        .gid(linkUpdateReqDTO.getNewGid())
                        .build();
                linkLocateStatsMapper.update(linkLocaleStatsDO, linkLocaleStatsUpdateWrapper);
                //update OS
                LambdaUpdateWrapper<LinkOSStatsDO> linkOsStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkOSStatsDO.class)
                        .eq(LinkOSStatsDO::getFullShortUrl, linkUpdateReqDTO.getFullShortUrl())
                        .eq(LinkOSStatsDO::getGid, linkDO.getGid())
                        .eq(LinkOSStatsDO::getDelFlag, 0);
                LinkOSStatsDO linkOsStatsDO = LinkOSStatsDO.builder()
                        .gid(linkUpdateReqDTO.getNewGid())
                        .build();
                linkOSStatsMapper.update(linkOsStatsDO, linkOsStatsUpdateWrapper);

                LambdaUpdateWrapper<LinkBrowserStatsDO> linkBrowserStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkBrowserStatsDO.class)
                        .eq(LinkBrowserStatsDO::getFullShortUrl, linkUpdateReqDTO.getFullShortUrl())
                        .eq(LinkBrowserStatsDO::getGid, linkDO.getGid())
                        .eq(LinkBrowserStatsDO::getDelFlag, 0);
                LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                        .gid(linkUpdateReqDTO.getNewGid())
                        .build();
                linkBrowserStatsMapper.update(linkBrowserStatsDO, linkBrowserStatsUpdateWrapper);
                LambdaUpdateWrapper<LinkDeviceStatsDO> linkDeviceStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkDeviceStatsDO.class)
                        .eq(LinkDeviceStatsDO::getFullShortUrl, linkUpdateReqDTO.getFullShortUrl())
                        .eq(LinkDeviceStatsDO::getGid, linkDO.getGid())
                        .eq(LinkDeviceStatsDO::getDelFlag, 0);
                LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
                        .gid(linkUpdateReqDTO.getNewGid())
                        .build();
                linkDeviceStatsMapper.update(linkDeviceStatsDO, linkDeviceStatsUpdateWrapper);
                LambdaUpdateWrapper<LinkNetworkStatsDO> linkNetworkStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkNetworkStatsDO.class)
                        .eq(LinkNetworkStatsDO::getFullShortUrl, linkUpdateReqDTO.getFullShortUrl())
                        .eq(LinkNetworkStatsDO::getGid, linkDO.getGid())
                        .eq(LinkNetworkStatsDO::getDelFlag, 0);
                LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
                        .gid(linkUpdateReqDTO.getNewGid())
                        .build();
                linkNetworkStatsMapper.update(linkNetworkStatsDO, linkNetworkStatsUpdateWrapper);
                LambdaUpdateWrapper<LinkAccessLogDO> linkAccessLogsUpdateWrapper = Wrappers.lambdaUpdate(LinkAccessLogDO.class)
                        .eq(LinkAccessLogDO::getFullShortUrl, linkUpdateReqDTO.getFullShortUrl())
                        .eq(LinkAccessLogDO::getGid, linkDO.getGid())
                        .eq(LinkAccessLogDO::getDelFlag, 0);
                LinkAccessLogDO linkAccessLogsDO = LinkAccessLogDO.builder()
                        .gid(linkUpdateReqDTO.getNewGid())
                        .build();
                linkAccessLogsMapper.update(linkAccessLogsDO, linkAccessLogsUpdateWrapper);
            } finally {
                writeLock.unlock();
            }
        }
        //检查短连接的更新前后有效期是否变化
        if (!Objects.equals(linkDO.getValidDateType(), linkUpdateReqDTO.getValidDateType()) ||
                !Objects.equals(linkDO.getValidDate(), linkUpdateReqDTO.getValidDate())) {
            stringRedisTemplate.delete(String.format(FULL_SHORT_URL_KEY, linkDO.getFullShortUrl()));//发生变化，则需要删除缓存中数据
            if (Objects.equals(linkUpdateReqDTO.getValidDateType(), 0) ||
                    linkUpdateReqDTO.getValidDate().isAfter(LocalDateTime.now())) {
                //删除404缓存
                stringRedisTemplate.delete(String.format(IS_NULL_FULL_SHORT_URL_KEY, linkDO.getFullShortUrl()));
            }
        }

    }


    public String generateLinkSuffix(LinkCreateReqDTO linkCreateReqDTO) {
        int cnt = 0;
        int maxTryCount = 10;
        String originUrl = linkCreateReqDTO.getOriginUrl();
        String shortLinkSuffix = null;
        while (true) {
            if (cnt >= maxTryCount) try {
                throw new ServerCloneException("操作频繁，请稍后再试");
            } catch (ServerCloneException e) {
                throw new RuntimeException(e);
            }
            originUrl = System.currentTimeMillis() + UUID.randomUUID().toString();//在高并发场景下，相同毫秒数可能会有多个重复值
            shortLinkSuffix = HashUtil.hashToBase62(originUrl);
            String fullShortLinkUrl = defaultDomain + '/' + shortLinkSuffix;
            if (!rBloomFilter.contains(fullShortLinkUrl)) {
                break;
            }
            cnt++;
        }

        return shortLinkSuffix;
    }

    /**
     * 获取原链接网站图标
     *
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

    private ShortLinkStatsRecordDTO builderLinkStatsRecordAndSetUser(String fullShortUrl, HttpServletRequest request, HttpServletResponse response) {
        AtomicBoolean uvFirstFlag = new AtomicBoolean();
        Cookie[] cookies = request.getCookies();
        Date currentTime = new Date();
        AtomicReference<String> uv = new AtomicReference<>();
        String redisUvKey = REDIS_UV_KEY + fullShortUrl;
        String redisUipKey = REDIS_UIP_KEY + fullShortUrl;

        Runnable addCookieTask = () -> {
            String uv_value = UUID.randomUUID().toString();
            uv.set(uv_value);
            Cookie cookie = new Cookie("uv", uv.get());
            cookie.setPath(StrUtil.sub(fullShortUrl, fullShortUrl.indexOf('/'), fullShortUrl.length()));
            cookie.setMaxAge(30 * 24 * 60 * 60);
            response.addCookie(cookie);
            uvFirstFlag.set(true);
            stringRedisTemplate.opsForSet().add(redisUvKey, uv.get());
        };

        if (ArrayUtil.isNotEmpty(cookies)) {
            Arrays.stream(cookies)
                    .filter(each -> Objects.equals(each.getName(), "uv"))
                    .findFirst()
                    .map(Cookie::getValue)
                    .ifPresentOrElse(each -> {
                        uv.set(each);
                        Long UVAdd = stringRedisTemplate.opsForSet().add(redisUvKey, each);
                        uvFirstFlag.set(UVAdd != null && UVAdd > 0); //true : 第一次访问这个页面, false : 不是第一次访问页面

                    }, addCookieTask);
        } else {
            addCookieTask.run();
        }
        String remoteAddr = LinkUtil.getIP( request);
        String os = LinkUtil.getOs( request);
        String browser = LinkUtil.getBrowser( request);
        String device = LinkUtil.getDevice( request);
        String network = LinkUtil.getNetwork(request);

        Long uipAdded = stringRedisTemplate.opsForSet().add(redisUipKey, remoteAddr);
        boolean uipFirstFlag = uipAdded != null && uipAdded > 0L;
        return ShortLinkStatsRecordDTO.builder()
                .fullShortUrl(fullShortUrl)
                .uv(uv.get())
                .uvFirstFlag(uvFirstFlag.get())
                .uipFirstFlag(uipFirstFlag)
                .remoteAddr(remoteAddr)
                .os(os)
                .browser(browser)
                .device(device)
                .network(network)
                .currentDate(new Date())
                .build();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void shortLinkStats(String fullShortUrl, String gid,ShortLinkStatsRecordDTO statsRecord) {
//       Map<String,String> producerMap = new HashMap<>();
//       producerMap.put("statsRecord", JSON.toJSONString(statsRecord));
       //暂时使用redission的延迟队列
       RReadWriteLock rReadWriteLock =  redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY,statsRecord.getFullShortUrl()));
       //获取统计数据之前先获取读锁
        RLock rLock = rReadWriteLock.readLock();
        fullShortUrl = statsRecord.getFullShortUrl();
        /**
         * 删除延迟队列：原因：当访问短连接时，统计数据的工作已经放入消息队列中进行处理。
         * 消息队列天然支持异步处理，无需额外的延迟机制，就能保证统计操作不会影响主流程
         * 锁的持有时间会大幅缩短，锁竞争的概率也会相应降低
         */
        rLock.lock();//由于我们系统已经进行限流了，此时可以阻塞等待写锁的释放
//        if(!rLock.tryLock()) {
//            //生产者发送消息
//            SendResult sendResult = delayShortLinkStatsMQProducer.sendMessage(statsRecord);
//            if(!Objects.equals(sendResult.getSendStatus(), SendStatus.SEND_OK)){
//                throw new ServiceException("延迟队列投递消息失败");
//            }
//            // delayShortLinkStatsProducer.send(statsRecord);
//            return;
//        }
        //已经获取到了读锁, 执行统计任务
        try {
            if(StrUtil.isBlank(gid)){

                LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
                ShortLinkGotoDO gotoDO = gotoMapper.selectOne(queryWrapper);
                gid = gotoDO.getGid();
            }
            /**
             * 获取时间日期等信息
             */
            Date currentTime = new Date();
            int hour = DateUtil.hour(currentTime,true);
            Week week = DateUtil.dayOfWeekEnum(currentTime);
            int weekValue = week.getIso8601Value();
            int uv_final = statsRecord.getUvFirstFlag() ? 1 : 0;
            int uip_final = statsRecord.getUipFirstFlag() ? 1 : 0;
            ShortLinkStatsDO statsDO = ShortLinkStatsDO.builder()
                    .gid(gid)
                    .fullShortUrl(fullShortUrl)
                    .pv(1)
                    .uv(uv_final) //uvFlag true: 第一次访问+1， 否则为0
                    .uip(uip_final)
                    .hour(hour)
                    .weekday(weekValue)
                    .date(currentTime)
                    .build();

            //调用高德地图API获取定位信息
            Map<String, Object> apiRequestParam = new HashMap<>();
            apiRequestParam.put("ip",statsRecord.getRemoteAddr());
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
            if("[]".equals(province)) province = "未知";
            if("[]".equals(city) ) city = "未知";
            LinkLocateStatsDO locateStatsDO = LinkLocateStatsDO.builder()
                    .cnt(1)
                    .country("中国")
                    .gid(gid)
                    .adcode(adcode)
                    .province(province)
                    .city(city)
                    .date(currentTime)
                    .fullShortUrl(fullShortUrl)
                    .build();
            shortLinkStatsMapper.shortLinkStats(statsDO);//更新uv，PV，uip
            linkLocateStatsMapper.add(locateStatsDO);//更新省份，地区
            /**
             * 更新操作系统
             */
            String os = statsRecord.getOs();
            LinkOSStatsDO osStatsDO = LinkOSStatsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .cnt(1)
                    .date(currentTime)
                    .os(os)
                    .build();
            linkOSStatsMapper.LinkOsStats(osStatsDO);

            /**
             * 更新浏览器信息
             */
            String browser = statsRecord.getBrowser();
            LinkBrowserStatsDO browserStatsDO = LinkBrowserStatsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .cnt(1)
                    .date(currentTime)
                    .browser(browser)
                    .build();
            linkBrowserStatsMapper.LinkBrowserStats(browserStatsDO);


            /**
             * 设备
             */
            String device = statsRecord.getDevice();
            LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
                    .device(device)
                    .cnt(1)
                    .gid(gid)
                    .fullShortUrl(fullShortUrl)
                    .date(currentTime)
                    .build();
            linkDeviceStatsMapper.LinkDeviceStats(linkDeviceStatsDO);

            /**
             * 网络
             */
            String network = statsRecord.getNetwork();
            LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
                    .network(network)
                    .cnt(1)
                    .gid(gid)
                    .fullShortUrl(fullShortUrl)
                    .date(currentTime)
                    .build();
            linkNetworkStatsMapper.LinkNetworkStats(linkNetworkStatsDO);

            /**
             * 更新日志
             *
             */
            LinkAccessLogDO accessLogDO = LinkAccessLogDO.builder()
                    .gid(gid)
                    .fullShortUrl(fullShortUrl)
                    .ip(statsRecord.getRemoteAddr())
                    .os(os)
                    .browser(browser)
                    .user(statsRecord.getUv())
                    .device(device)
                    .network(network)
                    .locate(String.join("-","中国",province,city))
                    .build();
            linkAccessLogsMapper.insert(accessLogDO);
            //更新link表total pv,uv,uip
            baseMapper.incrementStats(gid,fullShortUrl, 1,statsRecord.getUvFirstFlag()?1:0,statsRecord.getUipFirstFlag()?1:0);

            LinkTodayStatsDO todayStatsDO = LinkTodayStatsDO.builder()
                    .gid(gid)
                    .fullShortUrl(fullShortUrl)
                    .date(currentTime)
                    .todayPv(1)
                    .todayUv(uv_final)
                    .todayUip(uip_final)
                    .build();
            linkTodayStatsMapper.linkTodayStats(todayStatsDO);
        } catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            rLock.unlock();
        }
    }

}


