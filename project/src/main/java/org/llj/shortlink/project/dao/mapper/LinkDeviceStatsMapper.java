package org.llj.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.llj.shortlink.project.dao.entity.LinkAccessLogDO;
import org.llj.shortlink.project.dao.entity.LinkDeviceStatsDO;

@Mapper
public interface LinkDeviceStatsMapper extends BaseMapper<LinkAccessLogDO> {
    @Insert("INSERT INTO t_link_device_stats (full_short_url,gid,date,cnt,device, create_time,update_time,del_flag)\n " +
            "VALUES(#{fullShortUrl},#{gid},#{date},#{cnt},#{device},NOW(), NOW(),0) ON DUPLICATE KEY\n " +
            "UPDATE cnt = cnt + #{cnt}")
    void LinkDeviceStats(LinkDeviceStatsDO linkDeviceStatsDO);
}
