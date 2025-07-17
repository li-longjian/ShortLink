package org.llj.shortlink.project.dto.resp;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShortLinkPageRespDTO {
    /**
     * id
     */
    private Long id;

    /**
     * 短链接
     */
    private String shortUri;

    /**
     * 完整短链接
     */
    private String fullShortUrl;


    /**
     * 分组标识
     */
    private String gid;

    /**
     * 有效期
     */
    private LocalDateTime validDate;

    /**
     * 描述
     */
    @TableField("`describe`")
    private String describe;

    /**
     * 图标
     */
    private String favicon;
}
