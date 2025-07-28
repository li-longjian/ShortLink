package org.llj.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.llj.shortlink.project.common.dataBase.BaseDO;

import java.util.Date;

@Data
@TableName("t_link_access_stats")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShortLinkStatsDO extends BaseDO {
    private Long id;
    private String gid;
    private String fullShortUrl;
    private Date date;
    private Integer pv;
    private Integer uv;
    private Integer uip;
    private Integer hour;
    private Integer weekday;

}
