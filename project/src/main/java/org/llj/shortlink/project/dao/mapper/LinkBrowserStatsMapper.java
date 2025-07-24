package org.llj.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.llj.shortlink.project.dao.entity.LinkBrowserStatsDO;

@Mapper
public interface LinkBrowserStatsMapper extends BaseMapper<LinkBrowserStatsDO> {
    @Insert("INSERT INTO t_link_browser_stats (full_short_url,gid,date,cnt,browser, create_time,update_time,del_flag)\n " +
            "VALUES(#{fullShortUrl},#{gid},#{date},#{cnt},#{browser},NOW(), NOW(),0) ON DUPLICATE KEY\n " +
            "UPDATE cnt = cnt + #{cnt}")
    void LinkBrowserStats(LinkBrowserStatsDO linkBrowserStatsDO);
}
