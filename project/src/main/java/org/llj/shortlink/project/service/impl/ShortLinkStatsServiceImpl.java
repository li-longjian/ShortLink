package org.llj.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.llj.shortlink.project.dao.entity.*;
import org.llj.shortlink.project.dao.mapper.*;
import org.llj.shortlink.project.dto.req.LinkAccessGroupRecodeReqDTO;
import org.llj.shortlink.project.dto.req.LinkAccessRecodeReqDTO;
import org.llj.shortlink.project.dto.req.LinkStatsGroupReqDTO;
import org.llj.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import org.llj.shortlink.project.dto.resp.*;
import org.llj.shortlink.project.service.ShortLinkStatsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class ShortLinkStatsServiceImpl  implements ShortLinkStatsService {
    private final ShortLinkStatsMapper shortLinkStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkLocateStatsMapper linkLocateStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final LinkOSStatsMapper linkOSSStatsMapper;

    /**
     * 访问单个短链接指定时间内监控数据
     * @param requestParam 获取短链接监控数据入参
     * @return
     */
    @Override
    public ShortLinkStatsRespDTO oneShortLinkStats(ShortLinkStatsReqDTO requestParam) {
        //1. 基础访问详情
        List<ShortLinkStatsDO> shortLinkStatsDOS = shortLinkStatsMapper.listStatsByShortLink(requestParam);
        if(CollUtil.isEmpty(shortLinkStatsDOS)) return null;
        //基础访问数据
        ShortLinkStatsDO pvUvUipByShortLink = linkAccessLogsMapper.findPvUvUipByShortLink(requestParam);

        //2. 地区详情

        //get the detail info of one link in a province
        List<LinkLocateStatsDO> linkLocateStatsDOS = linkLocateStatsMapper.listLocaleByShortLink(requestParam);
        int locate_sum = linkLocateStatsDOS.stream().mapToInt(LinkLocateStatsDO::getCnt).sum();//get the sum of all link cnt between  start and endtime
        List<ShortLinkStatsLocateCNRespDTO> locateCNStats = new ArrayList<>();// store all response of locateCNRespDTO
        linkLocateStatsDOS.forEach(each ->{
            double ratio = each.getCnt() * 1.0 / locate_sum;
            double actualRation = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsLocateCNRespDTO locateCNRespDTO = ShortLinkStatsLocateCNRespDTO.builder()
                    .cnt(each.getCnt())
                    .ratio(actualRation)
                    .locate(each.getProvince())
                    .build();
            locateCNStats.add(locateCNRespDTO);

        });

        //3. 小时访问详情
        List<Integer> hourStats = new ArrayList<>();
        //a list between start and end time,  get the sum of PV of a link , group by hour
        List<ShortLinkStatsDO> linkStatsDOS_hour = shortLinkStatsMapper.listHourStatsByShortLink(requestParam);
        for(int i=0 ; i< 24;i++){
            AtomicInteger hour = new AtomicInteger(i);
            Integer hour_cnt = linkStatsDOS_hour.stream()
                    .filter(each -> ObjectUtil.equal(each.getHour(), hour.get()))
                    .findFirst()
                    .map(ShortLinkStatsDO::getPv)
                    .orElse(0);
            hourStats.add(hour_cnt);
        }
        //4. 高频IP访问详情
        List<ShortLinkStatsTopIpRespDTO> TopIPStats = new ArrayList<>();
        //get the top 5 ip and count between start time and end time
        List<HashMap<String, Object>> listTopIpByShortLink = linkAccessLogsMapper.listTopIpByShortLink(requestParam);
        listTopIpByShortLink.forEach(each->{
            ShortLinkStatsTopIpRespDTO respDTO = ShortLinkStatsTopIpRespDTO.builder()
                    .ip(each.get("ip").toString())
                    .cnt(Integer.parseInt(each.get("count").toString()))
                    .build();
            TopIPStats.add(respDTO);
        });
        //5. 一周访问详情
        //get the weekday, sum（PV） of weekday
        List<ShortLinkStatsDO> weekdayStatsList = shortLinkStatsMapper.listWeekdayStatsByShortLink(requestParam);
        List<Integer> weekStats = new ArrayList<>();
        for(int i=0;i<8;i++){
            AtomicInteger weekday = new AtomicInteger(i);
            int weekdayCnt = weekdayStatsList.stream()
                    .filter(each -> ObjectUtil.equal(each.getWeekday(), weekday.get()))
                    .mapToInt(ShortLinkStatsDO::getPv)
                    .findFirst()
                    .orElse(0);
            weekStats.add(weekdayCnt);
        }
        //6.浏览器访问详情: 统计某个时间段内，各个浏览器的访问次数
        List<HashMap<String, Object>> browserStatsList = linkBrowserStatsMapper.listBrowserStatsByShortLink(requestParam);
        List<ShortLinkStatsBrowserRespDTO> browserStats = new ArrayList<>();
        //统计总次数
        int sum_browser = browserStatsList.stream()
                .mapToInt(each -> Integer.parseInt(each.get("count").toString()))
                .sum();
        browserStatsList.forEach(each->{
            int each_count = Integer.parseInt(each.get("count").toString());
            double each_ratio = Math.round(each_count *1.0  / sum_browser * 100.0) / 100.0;
            ShortLinkStatsBrowserRespDTO browserRespDTO = ShortLinkStatsBrowserRespDTO.builder()
                    .browser(each.get("browser").toString())
                    .ratio(each_ratio)
                    .cnt(each_count)
                    .build();
            browserStats.add(browserRespDTO);
        });
        //7. 操作系统访问详情
        List<ShortLinkStatsOsRespDTO> osStats = new ArrayList<>();
        List<HashMap<String, Object>> listOsStatsByShortLink = linkOSSStatsMapper.listOsStatsByShortLink(requestParam);
        int osSum = listOsStatsByShortLink.stream()
                .mapToInt(each -> Integer.parseInt(each.get("count").toString()))
                .sum();
        listOsStatsByShortLink.forEach(each -> {
            double ratio = (double) Integer.parseInt(each.get("count").toString()) / osSum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsOsRespDTO osRespDTO = ShortLinkStatsOsRespDTO.builder()
                    .cnt(Integer.parseInt(each.get("count").toString()))
                    .os(each.get("os").toString())
                    .ratio(actualRatio)
                    .build();
            osStats.add(osRespDTO);
        });
        //8. 访客类型 ： 新访客 ， 旧访客
        HashMap<String, Object> uvTypeCntByShortLink = linkAccessLogsMapper.findUvTypeCntByShortLink(requestParam);
        int oldUserCount = Integer.parseInt(uvTypeCntByShortLink.get("oldUserCnt").toString());
        int newUserCount = Integer.parseInt(uvTypeCntByShortLink.get("newUserCnt").toString());
        int UserSum = oldUserCount + newUserCount;
        double oldRatio = oldUserCount * 1.0 / UserSum;
        double actualOldRatio = Math.round(oldRatio * 100.0) / 100.0;
        double newRatio = newUserCount * 1.0 / UserSum;
        double actualNewRatio = Math.round(newRatio * 100.0) / 100.0;
        List<ShortLinkStatsUvRespDTO> uvTypeStats = new ArrayList<>();
        ShortLinkStatsUvRespDTO newUvRespDTO = ShortLinkStatsUvRespDTO
                .builder()
                .uvType("newUser")
                .ratio(actualNewRatio)
                .cnt(newUserCount)
                .build();
        uvTypeStats.add(newUvRespDTO);
        ShortLinkStatsUvRespDTO oldUvRespDTO = ShortLinkStatsUvRespDTO
                .builder()
                .uvType("oldUser")
                .ratio(actualOldRatio)
                .cnt(oldUserCount)
                .build();
        uvTypeStats.add(oldUvRespDTO);
        // 9.访问设备类型详情
        List<ShortLinkStatsDeviceRespDTO> deviceStats = new ArrayList<>();
        List<LinkDeviceStatsDO> listDeviceStatsByShortLink = linkDeviceStatsMapper.listDeviceStatsByShortLink(requestParam);
        int deviceSum = listDeviceStatsByShortLink.stream()
                .mapToInt(LinkDeviceStatsDO::getCnt)
                .sum();
        listDeviceStatsByShortLink.forEach(each -> {
            double ratio = (double) each.getCnt() / deviceSum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsDeviceRespDTO deviceRespDTO = ShortLinkStatsDeviceRespDTO.builder()
                    .cnt(each.getCnt())
                    .device(each.getDevice())
                    .ratio(actualRatio)
                    .build();
            deviceStats.add(deviceRespDTO);
        });
        // 10 . 访问网络类型详情
        List<ShortLinkStatsNetworkRespDTO> networkStats = new ArrayList<>();
        List<LinkNetworkStatsDO> listNetworkStatsByShortLink = linkNetworkStatsMapper.listNetworkStatsByShortLink(requestParam);
        int networkSum = listNetworkStatsByShortLink.stream()
                .mapToInt(LinkNetworkStatsDO::getCnt)
                .sum();
        listNetworkStatsByShortLink.forEach(each -> {
            double ratio = (double) each.getCnt() / networkSum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsNetworkRespDTO networkRespDTO = ShortLinkStatsNetworkRespDTO.builder()
                    .cnt(each.getCnt())
                    .network(each.getNetwork())
                    .ratio(actualRatio)
                    .build();
            networkStats.add(networkRespDTO);
        });

        return ShortLinkStatsRespDTO
                .builder()
                .pv(pvUvUipByShortLink.getPv())
                .uv(pvUvUipByShortLink.getUv())
                .uip(pvUvUipByShortLink.getUip())
                .daily(BeanUtil.copyToList(shortLinkStatsDOS,ShortLinkStatsAccessDailyRespDTO.class))
                .localeCnStats(locateCNStats)
                .hourStats(hourStats)
                .topIpStats(TopIPStats)
                .weekdayStats(weekStats)
                .browserStats(browserStats)
                .osStats(osStats)
                .uvTypeStats(uvTypeStats)
                .deviceStats(deviceStats)
                .networkStats(networkStats)
                .build();
    }

    /**
     * 访问分组短链接指定时间内监控数据
     * @param requestParam 获取短链接监控数据入参
     * @return
     */
    @Override
    public ShortLinkStatsRespDTO groupShortLinkStats(LinkStatsGroupReqDTO requestParam) {

        List<ShortLinkStatsDO> listStatsByGroup  = shortLinkStatsMapper.listStatsByGroup(requestParam);
        if(CollUtil.isEmpty(listStatsByGroup )) return null;
        //基础访问数据
        ShortLinkStatsDO pvUvUipByShortLink = linkAccessLogsMapper.findPvUvUipByGroup(requestParam);
        //1. 基础访问详情
        List<ShortLinkStatsAccessDailyRespDTO> daily = new ArrayList<>();
        // 直接生成Date对象列表，不转换为字符串
        List<DateTime> rangeDates = DateUtil.rangeToList(
                DateUtil.parse(requestParam.getStartDate()),
                DateUtil.parse(requestParam.getEndDate()),
                DateField.DAY_OF_MONTH
        );
        rangeDates.forEach(each -> listStatsByGroup.stream()
                .filter(item -> Objects.equals(each, item.getDate()))
                .findFirst()
                .ifPresentOrElse(item -> {
                    ShortLinkStatsAccessDailyRespDTO accessDailyRespDTO = ShortLinkStatsAccessDailyRespDTO.builder()
                            .date(each)
                            .pv(item.getPv())
                            .uv(item.getUv())
                            .uip(item.getUip())
                            .build();
                    daily.add(accessDailyRespDTO);
                }, () -> {
                    ShortLinkStatsAccessDailyRespDTO accessDailyRespDTO = ShortLinkStatsAccessDailyRespDTO.builder()
                            .date(each)
                            .pv(0)
                            .uv(0)
                            .uip(0)
                            .build();
                    daily.add(accessDailyRespDTO);
                }));
        //2. 地区详情
        //get the detail info of one link in a province
        List<LinkLocateStatsDO> linkLocateStatsDOS = linkLocateStatsMapper.listLocaleByGroup(requestParam);
        int locate_sum = linkLocateStatsDOS.stream().mapToInt(LinkLocateStatsDO::getCnt).sum();//get the sum of all link cnt between  start and endtime
        List<ShortLinkStatsLocateCNRespDTO> locateCNStats = new ArrayList<>();// store all response of locateCNRespDTO
        linkLocateStatsDOS.forEach(each ->{
            double ratio = each.getCnt() * 1.0 / locate_sum;
            double actualRation = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsLocateCNRespDTO locateCNRespDTO = ShortLinkStatsLocateCNRespDTO.builder()
                    .cnt(each.getCnt())
                    .ratio(actualRation)
                    .locate(each.getProvince())
                    .build();
            locateCNStats.add(locateCNRespDTO);

        });

        //3. 小时访问详情
        List<Integer> hourStats = new ArrayList<>();
        //a list between start and end time,  get the sum of PV of a link , group by hour
        List<ShortLinkStatsDO> linkStatsDOS_hour = shortLinkStatsMapper.listHourStatsByGroup(requestParam);
        for(int i=0 ; i< 24;i++){
            AtomicInteger hour = new AtomicInteger(i);
            Integer hour_cnt = linkStatsDOS_hour.stream()
                    .filter(each -> ObjectUtil.equal(each.getHour(), hour.get()))
                    .findFirst()
                    .map(ShortLinkStatsDO::getPv)
                    .orElse(0);
            hourStats.add(hour_cnt);
        }
        //4. 高频IP访问详情
        List<ShortLinkStatsTopIpRespDTO> TopIPStats = new ArrayList<>();
        //get the top 5 ip and count between start time and end time
        List<HashMap<String, Object>> listTopIpByShortLink = linkAccessLogsMapper.listTopIpByGroup(requestParam);
        listTopIpByShortLink.forEach(each->{
            ShortLinkStatsTopIpRespDTO respDTO = ShortLinkStatsTopIpRespDTO.builder()
                    .ip(each.get("ip").toString())
                    .cnt(Integer.parseInt(each.get("count").toString()))
                    .build();
            TopIPStats.add(respDTO);
        });
        //5. 一周访问详情
        //get the weekday, sum（PV） of weekday
        List<ShortLinkStatsDO> weekdayStatsList = shortLinkStatsMapper.listWeekdayStatsByGroup(requestParam);
        List<Integer> weekStats = new ArrayList<>();
        for(int i=0;i<8;i++){
            AtomicInteger weekday = new AtomicInteger(i);
            int weekdayCnt = weekdayStatsList.stream()
                    .filter(each -> ObjectUtil.equal(each.getWeekday(), weekday.get()))
                    .mapToInt(ShortLinkStatsDO::getPv)
                    .findFirst()
                    .orElse(0);
            weekStats.add(weekdayCnt);
        }
        //6.浏览器访问详情: 统计某个时间段内，各个浏览器的访问次数
        List<HashMap<String, Object>> browserStatsList = linkBrowserStatsMapper.listBrowserStatsByGroup(requestParam);
        List<ShortLinkStatsBrowserRespDTO> browserStats = new ArrayList<>();
        //统计总次数
        int sum_browser = browserStatsList.stream()
                .mapToInt(each -> Integer.parseInt(each.get("count").toString()))
                .sum();
        browserStatsList.forEach(each->{
            int each_count = Integer.parseInt(each.get("count").toString());
            double each_ratio = Math.round(each_count *1.0  / sum_browser * 100.0) / 100.0;
            ShortLinkStatsBrowserRespDTO browserRespDTO = ShortLinkStatsBrowserRespDTO.builder()
                    .browser(each.get("browser").toString())
                    .ratio(each_ratio)
                    .cnt(each_count)
                    .build();
            browserStats.add(browserRespDTO);
        });
        //7. 操作系统访问详情
        List<ShortLinkStatsOsRespDTO> osStats = new ArrayList<>();
        List<HashMap<String, Object>> listOsStatsByShortLink = linkOSSStatsMapper.listOsStatsByGroup(requestParam);
        int osSum = listOsStatsByShortLink.stream()
                .mapToInt(each -> Integer.parseInt(each.get("count").toString()))
                .sum();
        listOsStatsByShortLink.forEach(each -> {
            double ratio = (double) Integer.parseInt(each.get("count").toString()) / osSum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsOsRespDTO osRespDTO = ShortLinkStatsOsRespDTO.builder()
                    .cnt(Integer.parseInt(each.get("count").toString()))
                    .os(each.get("os").toString())
                    .ratio(actualRatio)
                    .build();
            osStats.add(osRespDTO);
        });
        //8. 访客类型 ： 新访客 ， 旧访客
        HashMap<String, Object> uvTypeCntByShortLink = linkAccessLogsMapper.findUvTypeCntByGroup(requestParam);
        int oldUserCount = Integer.parseInt(uvTypeCntByShortLink.get("oldUserCnt").toString());
        int newUserCount = Integer.parseInt(uvTypeCntByShortLink.get("newUserCnt").toString());
        int UserSum = oldUserCount + newUserCount;
        double oldRatio = oldUserCount * 1.0 / UserSum;
        double actualOldRatio = Math.round(oldRatio * 100.0) / 100.0;
        double newRatio = newUserCount * 1.0 / UserSum;
        double actualNewRatio = Math.round(newRatio * 100.0) / 100.0;
        List<ShortLinkStatsUvRespDTO> uvTypeStats = new ArrayList<>();
        ShortLinkStatsUvRespDTO newUvRespDTO = ShortLinkStatsUvRespDTO
                .builder()
                .uvType("newUser")
                .ratio(actualNewRatio)
                .cnt(newUserCount)
                .build();
        uvTypeStats.add(newUvRespDTO);
        ShortLinkStatsUvRespDTO oldUvRespDTO = ShortLinkStatsUvRespDTO
                .builder()
                .uvType("oldUser")
                .ratio(actualOldRatio)
                .cnt(oldUserCount)
                .build();
        uvTypeStats.add(oldUvRespDTO);
        // 9.访问设备类型详情
        List<ShortLinkStatsDeviceRespDTO> deviceStats = new ArrayList<>();
        List<LinkDeviceStatsDO> listDeviceStatsByShortLink = linkDeviceStatsMapper.listDeviceStatsByGroup(requestParam);
        int deviceSum = listDeviceStatsByShortLink.stream()
                .mapToInt(LinkDeviceStatsDO::getCnt)
                .sum();
        listDeviceStatsByShortLink.forEach(each -> {
            double ratio = (double) each.getCnt() / deviceSum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsDeviceRespDTO deviceRespDTO = ShortLinkStatsDeviceRespDTO.builder()
                    .cnt(each.getCnt())
                    .device(each.getDevice())
                    .ratio(actualRatio)
                    .build();
            deviceStats.add(deviceRespDTO);
        });
        // 10 . 访问网络类型详情
        List<ShortLinkStatsNetworkRespDTO> networkStats = new ArrayList<>();
        List<LinkNetworkStatsDO> listNetworkStatsByShortLink = linkNetworkStatsMapper.listNetworkStatsByGroup(requestParam);
        int networkSum = listNetworkStatsByShortLink.stream()
                .mapToInt(LinkNetworkStatsDO::getCnt)
                .sum();
        listNetworkStatsByShortLink.forEach(each -> {
            double ratio = (double) each.getCnt() / networkSum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsNetworkRespDTO networkRespDTO = ShortLinkStatsNetworkRespDTO.builder()
                    .cnt(each.getCnt())
                    .network(each.getNetwork())
                    .ratio(actualRatio)
                    .build();
            networkStats.add(networkRespDTO);
        });

        return ShortLinkStatsRespDTO
                .builder()
                .pv(pvUvUipByShortLink.getPv())
                .uv(pvUvUipByShortLink.getUv())
                .uip(pvUvUipByShortLink.getUip())
                .daily(daily)
                .localeCnStats(locateCNStats)
                .hourStats(hourStats)
                .topIpStats(TopIPStats)
                .weekdayStats(weekStats)
                .browserStats(browserStats)
                .osStats(osStats)
                .uvTypeStats(uvTypeStats)
                .deviceStats(deviceStats)
                .networkStats(networkStats)
                .build();
    }

    /**
     * 分组访问记录分页查询
     * @param requestParam
     * @return
     */
    @Override
    public IPage<LinkAccessRecodeRespDTO> linkAccessGroupStatsPage(LinkAccessGroupRecodeReqDTO requestParam) {
        LambdaQueryWrapper<LinkAccessLogDO> queryWrapper = Wrappers.lambdaQuery(LinkAccessLogDO.class)
                .eq(LinkAccessLogDO::getGid, requestParam.getGid())
                .eq(LinkAccessLogDO::getDelFlag, 0)
                .between(LinkAccessLogDO::getCreateTime,requestParam.getStartDate(),requestParam.getEndDate())
                .orderByDesc(LinkAccessLogDO::getCreateTime);
        IPage<LinkAccessLogDO> linkAccessLogPage = linkAccessLogsMapper.selectPage(requestParam, queryWrapper);
        IPage<LinkAccessRecodeRespDTO> resultPage = linkAccessLogPage.convert(each -> BeanUtil.toBean(each, LinkAccessRecodeRespDTO.class));
        List<String> uvList = resultPage.getRecords().stream()
                .map(LinkAccessRecodeRespDTO::getUser)
                .toList();//得到了结果集中用户列表, 为了下一步判断此用户是新用户或者老用户

        /**
         * 得到用户类型：新访客，旧访客
         */
        List<HashMap<String, Object>> uvTypeInfo = linkAccessLogsMapper.findGroupUvTypeByUser(
                requestParam.getGid(),
                requestParam.getStartDate(),
                requestParam.getEndDate(),
                uvList
        );
        resultPage.getRecords().forEach(each -> {
            String uvType = uvTypeInfo.stream()
                    .filter(item -> ObjectUtil.equal(each.getUser(), item.get("user")))
                    .findFirst()
                    .map(item -> item.get("uvType").toString())
                    .orElse("旧访客");
            each.setUvType(uvType);
        });
        return resultPage;
    }

    /**
     * 访问记录分页查询
     * @param requestParam
     * @return
     */
    @Override
    @Transactional
    public IPage<LinkAccessRecodeRespDTO> linkAccessStatsPage(LinkAccessRecodeReqDTO requestParam) {
        LambdaQueryWrapper<LinkAccessLogDO> queryWrapper = Wrappers.lambdaQuery(LinkAccessLogDO.class)
                .eq(LinkAccessLogDO::getGid, requestParam.getGid())
                .eq(LinkAccessLogDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(LinkAccessLogDO::getDelFlag, 0)
                .between(LinkAccessLogDO::getCreateTime,requestParam.getStartDate(),requestParam.getEndDate())
                .orderByDesc(LinkAccessLogDO::getCreateTime);

        IPage<LinkAccessLogDO> linkAccessLogPage = linkAccessLogsMapper.selectPage(requestParam, queryWrapper);
        IPage<LinkAccessRecodeRespDTO> resultPage = linkAccessLogPage.convert(each -> BeanUtil.toBean(each, LinkAccessRecodeRespDTO.class));
        List<String> uvList = resultPage.getRecords().stream()
                .map(LinkAccessRecodeRespDTO::getUser)
                .toList();//得到了结果集中用户列表, 为了下一步判断此用户是新用户或者老用户

        /**
         * 得到用户类型：新访客，旧访客
         */
        List<HashMap<String, Object>> uvTypeInfo = linkAccessLogsMapper.findUvTypeByUser(
                requestParam.getGid(),
                requestParam.getFullShortUrl(),
                requestParam.getStartDate(),
                requestParam.getEndDate(),
                uvList
        );
        resultPage.getRecords().forEach(each -> {
            String uvType = uvTypeInfo.stream()
                    .filter(item -> ObjectUtil.equal(each.getUser(), item.get("user")))
                    .findFirst()
                    .map(item -> item.get("uvType").toString())
                    .orElse("旧访客");
            each.setUvType(uvType);
        });
        return resultPage;
    }


}
