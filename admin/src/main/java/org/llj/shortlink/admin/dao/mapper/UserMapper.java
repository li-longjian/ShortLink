package org.llj.shortlink.admin.dao.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.llj.shortlink.admin.dao.entity.UserDo;

@Mapper
public interface UserMapper extends BaseMapper<UserDo> {
}
