package org.llj.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.llj.shortlink.project.common.dataBase.BaseDO;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_group")
public class GroupDO extends BaseDO {
    private Long id;
    private String gid;
    private String name;
    private String username;
    private Integer ordered;
}
