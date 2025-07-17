package org.llj.shortlink.admin.common.dataBase;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BaseDO {
    @TableField(fill =  FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill =  FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableField(fill = FieldFill.INSERT)
    private int delFlag;
}
