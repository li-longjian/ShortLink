package org.llj.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.llj.shortlink.project.dao.entity.LinkOSStatsDO;
import org.llj.shortlink.project.dto.req.ShortLinkStatsReqDTO;

import java.util.HashMap;
import java.util.List;

@Mapper
public interface LinkOSStatsMapper extends BaseMapper<LinkOSStatsDO> {
    @Insert("INSERT INTO t_link_os_stats (full_short_url,gid,date,cnt,os, create_time,update_time,del_flag)\n " +
            "VALUES(#{fullShortUrl},#{gid},#{date},#{cnt},#{os},NOW(), NOW(),0) ON DUPLICATE KEY\n " +
            "UPDATE cnt = cnt + #{cnt}")
    void LinkOsStats(LinkOSStatsDO linkOSStatsDO);

    /**
     * 根据短链接获取指定日期内操作系统监控数据
     */
    @Select("SELECT " +
            "    os, " +
            "    SUM(cnt) AS count " +
            "FROM " +
            "    t_link_os_stats " +
            "WHERE " +
            "    full_short_url = #{param.fullShortUrl} " +
            "    AND gid = #{param.gid} " +
            "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    full_short_url, gid, date, os;")
    List<HashMap<String, Object>> listOsStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);
}
