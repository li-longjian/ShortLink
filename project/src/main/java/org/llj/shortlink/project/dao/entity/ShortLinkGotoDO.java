package org.llj.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

@Data
@TableName("t_link_goto")
@Builder
public class ShortLinkGotoDO {
    private Long id;
    private String gid;
    private String fullShortUrl;
}
