package org.llj.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.llj.shortlink.project.dao.entity.LinkAccessLogDO;
import org.llj.shortlink.project.dao.entity.LinkNetworkStatsDO;
import org.llj.shortlink.project.dto.req.LinkStatsGroupReqDTO;
import org.llj.shortlink.project.dto.req.ShortLinkStatsReqDTO;

import java.util.List;

@Mapper
public interface LinkNetworkStatsMapper extends BaseMapper<LinkAccessLogDO> {
    @Insert("INSERT INTO t_link_network_stats (full_short_url,gid,date,cnt,network, create_time,update_time,del_flag)\n " +
            "VALUES(#{fullShortUrl},#{gid},#{date},#{cnt},#{network},NOW(), NOW(),0) ON DUPLICATE KEY\n " +
            "UPDATE cnt = cnt + #{cnt}")
    void LinkNetworkStats(LinkNetworkStatsDO linkNetworkStatsDO);

    /**
     * 根据短链接获取指定日期内访问网络监控数据
     */
    @Select("SELECT " +
            "    network, " +
            "    SUM(cnt) AS cnt " +
            "FROM " +
            "    t_link_network_stats " +
            "WHERE " +
            "    full_short_url = #{param.fullShortUrl} " +
            "    AND gid = #{param.gid} " +
            "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    full_short_url, gid, network;")
    List<LinkNetworkStatsDO> listNetworkStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);

    /**
     * 根据短链接获取指定日期内访问网络监控数据
     */
    @Select("SELECT " +
            "    network, " +
            "    SUM(cnt) AS cnt " +
            "FROM " +
            "    t_link_network_stats " +
            "WHERE " +
            "    gid = #{param.gid} " +
            "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "   gid, network;")
    List<LinkNetworkStatsDO> listNetworkStatsByGroup(@Param("param") LinkStatsGroupReqDTO requestParam);
}
