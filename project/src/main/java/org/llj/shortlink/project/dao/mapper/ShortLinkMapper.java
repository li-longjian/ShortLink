package org.llj.shortlink.project.dao.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.llj.shortlink.project.dao.entity.ShortLinkDO;
@Mapper
public interface ShortLinkMapper extends BaseMapper<ShortLinkDO> {

    @Update("update t_link set total_pv = total_pv + #{pv}, total_uv = total_uv + #{uv}, total_uip = total_uip + #{uip} where gid = #{gid} and full_short_url = #{fullShortUrl}")
    void incrementStats(@Param("gid") String gid,@Param("fullShortUrl") String fullShortUrl , @Param("pv")Integer pv,@Param("uv") Integer uv,@Param("uip") Integer uip);
}
