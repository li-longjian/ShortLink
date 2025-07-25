package org.llj.shortlink.project.dao.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.llj.shortlink.project.dao.entity.ShortLinkStatsDO;
import org.llj.shortlink.project.dto.req.LinkStatsGroupReqDTO;
import org.llj.shortlink.project.dto.req.ShortLinkStatsReqDTO;

import java.util.List;

/**
 * 短连接基础访问监控数据
 */
@Mapper
public interface ShortLinkStatsMapper extends BaseMapper<ShortLinkStatsDO> {

    @Insert("INSERT INTO t_link_access_stats (full_short_url,gid,date,pv, uv,uip,hour,weekday, create_time,update_time,del_flag)\n" +
            "VALUES(#{fullShortUrl},#{gid},#{date}," +
            "#{pv},#{uv},#{uip},#{hour},#{weekday},NOW(), NOW(),0) ON DUPLICATE KEY\n" +
            "UPDATE pv = pv + #{pv},uv = uv + #{uv},uip = uip + #{uip}")
    void shortLinkStats(ShortLinkStatsDO shortLinkStatsDO);

    /**
     * 根据短链接获取指定日期内基础监控数据
     */
    @Select("SELECT " +
            "    date, " +
            "    SUM(pv) AS pv, " +
            "    SUM(uv) AS uv, " +
            "    SUM(uip) AS uip " +
            "FROM " +
            "    t_link_access_stats " +
            "WHERE " +
            "    full_short_url = #{param.fullShortUrl} " +
            "    AND gid = #{param.gid} " +
            "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    full_short_url, gid, date;")
    List<ShortLinkStatsDO> listStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);
    /**
     * 根据分组获取指定日期内基础监控数据
     */
    @Select("SELECT " +
            "    date, " +
            "    SUM(pv) AS pv, " +
            "    SUM(uv) AS uv, " +
            "    SUM(uip) AS uip " +
            "FROM " +
            "    t_link_access_stats " +
            "WHERE " +
            "    gid = #{param.gid} " +
            "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            " gid, date;")
    List<ShortLinkStatsDO> listStatsByGroup(@Param("param") LinkStatsGroupReqDTO requestParam);
    /**
     * 根据短链接获取指定日期内小时基础监控数据
     */
    @Select("SELECT " +
            "    hour, " +
            "    SUM(pv) AS pv " +
            "FROM " +
            "    t_link_access_stats " +
            "WHERE " +
            "    full_short_url = #{param.fullShortUrl} " +
            "    AND gid = #{param.gid} " +
            "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    full_short_url, gid, hour;")
    List<ShortLinkStatsDO> listHourStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);
    /**
     * 根据短链接获取指定日期内周基础监控数据
     */
    @Select("SELECT " +
            "    weekday, " +
            "    SUM(pv) AS pv " +
            "FROM " +
            "    t_link_access_stats " +
            "WHERE " +
            "    full_short_url = #{param.fullShortUrl} " +
            "    AND gid = #{param.gid} " +
            "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    full_short_url, gid, weekday;")
    List<ShortLinkStatsDO> listWeekdayStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);
    /**
     * 根据分组获取指定日期内小时基础监控数据
     */
    @Select("SELECT " +
            "    hour, " +
            "    SUM(pv) AS pv " +
            "FROM " +
            "    t_link_access_stats " +
            "WHERE " +
            "  gid = #{param.gid} " +
            "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "  gid, hour;")
    List<ShortLinkStatsDO> listHourStatsByGroup(@Param("param") LinkStatsGroupReqDTO requestParam);
    /**
     * 根据分组获取指定日期内周基础监控数据
     */
    @Select("SELECT " +
            "    weekday, " +
            "    SUM(pv) AS pv " +
            "FROM " +
            "    t_link_access_stats " +
            "WHERE " +
            "    gid = #{param.gid} " +
            "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "   gid, weekday;")
    List<ShortLinkStatsDO> listWeekdayStatsByGroup(@Param("param") LinkStatsGroupReqDTO requestParam);

}
