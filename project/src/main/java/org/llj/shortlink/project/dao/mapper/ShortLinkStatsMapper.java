package org.llj.shortlink.project.dao.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.llj.shortlink.project.dao.entity.ShortLinkStatsDO;

@Mapper
public interface ShortLinkStatsMapper extends BaseMapper<ShortLinkStatsDO> {

    @Insert("INSERT INTO t_link_access_stats (full_short_url,gid,date,pv, uv,uip,hour,weekday, create_time,update_time,del_flag)\n" +
            "VALUES(#{fullShortUrl},#{gid},#{date}," +
            "#{pv},#{uv},#{uip},#{hour},#{weekday},NOW(), NOW(),0) ON DUPLICATE KEY\n" +
            "UPDATE pv = pv + #{pv},uv = uv + #{uv},uip = uip + #{uip}")
    void shortLinkStats(ShortLinkStatsDO shortLinkStatsDO);

}
