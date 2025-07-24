package org.llj.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.llj.shortlink.project.dao.entity.LinkAccessLogDO;

@Mapper
public interface LinkAccessLogsMapper extends BaseMapper<LinkAccessLogDO> {
    @Insert("INSERT INTO t_link_access_logs (full_short_url,gid,user,browser,os,ip, create_time,update_time,del_flag)\n " +
            "VALUES(#{fullShortUrl},#{gid},#{user},#{browser},#{os},#{ip},NOW(), NOW(),0)")
    void LinkAccessLogs(LinkAccessLogDO linkAccessLogDO);
}
