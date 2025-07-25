package org.llj.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.llj.shortlink.project.dao.entity.LinkLocateStatsDO;
import org.llj.shortlink.project.dto.req.ShortLinkStatsReqDTO;

import java.util.List;


@Mapper
public interface LinkLocateStatsMapper extends BaseMapper<LinkLocateStatsDO> {

    @Insert("INSERT INTO t_link_locate_stats (full_short_url,gid,date,cnt, province,city,adcode,country, create_time,update_time,del_flag)\n " +
            "VALUES(#{fullShortUrl},#{gid},#{date},#{cnt},#{province},#{city},#{adcode},#{country},NOW(), NOW(),0) ON DUPLICATE KEY\n " +
            "UPDATE cnt = cnt + #{cnt}")
    void add(LinkLocateStatsDO linkLocateStatsDO);

    /**
     * 根据短链接获取指定日期内基础监控数据
     */
    @Select("SELECT " +
            "    province, " +
            "    SUM(cnt) AS cnt " +
            "FROM " +
            "    t_link_locate_stats " +
            "WHERE " +
            "    full_short_url = #{param.fullShortUrl} " +
            "    AND gid = #{param.gid} " +
            "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    full_short_url, gid, province;")
    List<LinkLocateStatsDO> listLocaleByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);
}
