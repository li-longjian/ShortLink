package org.llj.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.llj.shortlink.project.dao.entity.LinkTodayStatsDO;

@Mapper
public interface LinkTodayStatsMapper extends BaseMapper<LinkTodayStatsDO> {

    @Insert("insert into  t_link_stats_today(gid,full_short_url,date,today_pv,today_uv,today_uip,create_time,update_time,del_flag)\n " +
            "VALUES(#{gid},#{fullShortUrl},#{date},#{todayPv},#{todayUv},#{todayUip},NOW(), NOW(),0) ON DUPLICATE KEY\n " +
            "UPDATE today_pv = today_pv + #{todayPv},today_uv = today_uv + #{todayUv},today_uip = today_uip + #{todayUip}")
    void linkTodayStats(LinkTodayStatsDO linkTodayStatsDO);
}
