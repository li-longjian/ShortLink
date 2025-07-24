package org.llj.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.llj.shortlink.project.dao.entity.LinkLocateStatsDO;


@Mapper
public interface LinkLocateStatsMapper extends BaseMapper<LinkLocateStatsDO> {

    @Insert("INSERT INTO t_link_locate_stats (full_short_url,gid,date,cnt, province,city,adcode,country, create_time,update_time,del_flag)\n " +
            "VALUES(#{fullShortUrl},#{gid},#{date},#{cnt},#{province},#{city},#{adcode},#{country},NOW(), NOW(),0) ON DUPLICATE KEY\n " +
            "UPDATE cnt = cnt + #{cnt}")
    void add(LinkLocateStatsDO linkLocateStatsDO);
}
