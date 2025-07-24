package org.llj.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.llj.shortlink.project.dao.entity.LinkOSStatsDO;

@Mapper
public interface LinkOSStatsMapper extends BaseMapper<LinkOSStatsDO> {
    @Insert("INSERT INTO t_link_os_stats (full_short_url,gid,date,cnt,os, create_time,update_time,del_flag)\n " +
            "VALUES(#{fullShortUrl},#{gid},#{date},#{cnt},#{os},NOW(), NOW(),0) ON DUPLICATE KEY\n " +
            "UPDATE cnt = cnt + #{cnt}")
    void LinkOsStats(LinkOSStatsDO linkOSStatsDO);
}
