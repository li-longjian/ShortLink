package org.llj.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.llj.shortlink.project.dao.entity.LinkAccessLogDO;
import org.llj.shortlink.project.dao.entity.ShortLinkStatsDO;
import org.llj.shortlink.project.dto.req.ShortLinkStatsReqDTO;

import java.util.HashMap;
import java.util.List;

@Mapper
public interface LinkAccessLogsMapper extends BaseMapper<LinkAccessLogDO> {
    /**
     * 插入一条日志
     * @param linkAccessLogDO
     */
    @Insert("INSERT INTO t_link_access_logs (full_short_url,gid,user,browser,os,ip,device,network,locate, create_time,update_time,del_flag)\n " +
            "VALUES(#{fullShortUrl},#{gid},#{user},#{browser},#{os},#{ip},#{device},#{network},#{locate},NOW(), NOW(),0)")
    void LinkAccessLogs(LinkAccessLogDO linkAccessLogDO);


    /**
     * 根据短链接获取指定日期内高频访问IP数据
     */
    @Select("SELECT " +
            "    ip, " +
            "    COUNT(ip) AS count " +
            "FROM " +
            "    t_link_access_logs " +
            "WHERE " +
            "    full_short_url = #{param.fullShortUrl} " +
            "    AND gid = #{param.gid} " +
            "    AND create_time BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    full_short_url, gid, ip " +
            "ORDER BY " +
            "    count DESC " +
            "LIMIT 5;")
    List<HashMap<String, Object>> listTopIpByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);

    /**
     * 根据短链接获取指定日期内新旧访客数据
     */
    @Select("SELECT " +
            "    SUM(old_user) AS oldUserCnt, " +
            "    SUM(new_user) AS newUserCnt " +
            "FROM ( " +
            "    SELECT " +
            "        CASE WHEN COUNT(DISTINCT DATE(create_time)) > 1 THEN 1 ELSE 0 END AS old_user, " +
            "        CASE WHEN COUNT(DISTINCT DATE(create_time)) = 1 AND MAX(create_time) >= #{param.startDate} AND MAX(create_time) <= #{param.endDate} THEN 1 ELSE 0 END AS new_user " +
            "    FROM " +
            "        t_link_access_logs " +
            "    WHERE " +
            "        full_short_url = #{param.fullShortUrl} " +
            "        AND gid = #{param.gid} " +
            "    GROUP BY " +
            "        user " +
            ") AS user_counts;")
    HashMap<String, Object> findUvTypeCntByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);

    /**
     * 使用用户信息通过日志创建时间判断是否为新访客或者旧访客
     * @param gid
     * @param fullShortUrl
     * @param startDate
     * @param endDate
     * @param userList
     * @return
     */
    @Select("<script> " +
            "SELECT " +
            "    user, " +
            "    CASE " +
            "        WHEN MIN(create_time) BETWEEN #{startDate} AND #{endDate} THEN '新访客' " +
            "        ELSE '老访客' " +
            "    END AS uvType " +
            "FROM " +
            "    t_link_access_logs " +
            "WHERE " +
            "    full_short_url = #{fullShortUrl} " +
            "    AND gid = #{gid} " +
            "    AND user IN " +
            "    <foreach item='item' index='index' collection='userList' open='(' separator=',' close=')'> " +
            "        #{item} " +
            "    </foreach> " +
            "GROUP BY " +
            "    user;" +
            "    </script>"
    )
    List<HashMap<String, Object>> findUvTypeByUser(@Param("gid")String gid,@Param("fullShortUrl")String fullShortUrl,@Param("startDate")String startDate,@Param("endDate")String endDate,@Param("userList")List<String> userList);

    @Select("select count(user) as pv, count(distinct  user) as uv, count(distinct  ip ) as uip\n " +
            "from t_link_access_logs\n " +
            "where gid = #{gid} and full_short_url = #{fullShortUrl} and create_time between #{startDate} and #{endDate}\n " +
            "group by full_short_url,gid")
    ShortLinkStatsDO findPvUvUipByShortLink(ShortLinkStatsReqDTO requestParam);

}
