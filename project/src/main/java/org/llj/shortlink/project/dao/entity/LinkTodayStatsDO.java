package org.llj.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import org.llj.shortlink.project.common.dataBase.BaseDO;

import java.util.Date;

@Data
@TableName("t_link_stats_today")
@Builder
public class LinkTodayStatsDO extends BaseDO {
    private Long id;
    private String gid;
    private String fullShortUrl;
    private Date date;
    private Integer todayPv;
    private Integer todayUv;
    private Integer todayUip;
}
