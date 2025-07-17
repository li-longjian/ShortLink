package org.llj.shortlink.admin.dao.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.llj.shortlink.admin.common.dataBase.BaseDO;

@Data
@TableName("t_user")
public class UserDo extends BaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String mail;
    private int deletionTime;
}
