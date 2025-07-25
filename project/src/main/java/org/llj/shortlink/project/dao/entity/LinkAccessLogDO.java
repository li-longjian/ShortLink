package org.llj.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.llj.shortlink.project.common.dataBase.BaseDO;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("t_link_access_logs")
public class LinkAccessLogDO extends BaseDO {
    private Long id;
    private String fullShortUrl;
    private String gid;
    private String browser;
    private String user;
    private String ip;
    private String os;
    private String device;
    private String locate;
    private String network;
}
