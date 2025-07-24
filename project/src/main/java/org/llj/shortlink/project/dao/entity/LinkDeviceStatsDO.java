package org.llj.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.llj.shortlink.project.common.dataBase.BaseDO;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("t_link_device_stats")
public class LinkDeviceStatsDO extends BaseDO {
    private Long id;
    private String fullShortUrl;
    private String gid;
    private String device;
    private Integer cnt;
    private Date date;
}
