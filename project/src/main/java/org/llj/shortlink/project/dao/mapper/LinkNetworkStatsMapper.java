package org.llj.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.llj.shortlink.project.dao.entity.LinkAccessLogDO;
import org.llj.shortlink.project.dao.entity.LinkNetworkStatsDO;

@Mapper
public interface LinkNetworkStatsMapper extends BaseMapper<LinkAccessLogDO> {
    @Insert("INSERT INTO t_link_network_stats (full_short_url,gid,date,cnt,network, create_time,update_time,del_flag)\n " +
            "VALUES(#{fullShortUrl},#{gid},#{date},#{cnt},#{network},NOW(), NOW(),0) ON DUPLICATE KEY\n " +
            "UPDATE cnt = cnt + #{cnt}")
    void LinkNetworkStats(LinkNetworkStatsDO linkNetworkStatsDO);
}
