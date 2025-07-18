package org.llj.shortlink.project.dto.req;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LinkUpdateReqDTO {


    /**
     * 短链接
     */
    private String shortUri;

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 原始链接
     */
    private String originUrl;


    /**
     * 分组标识 原始gid
     */
    private String gid;

    /**
     * 需要修改的新gid
     */
    private  String newGid;

    /**
     * 有效期类型 0：永久有效 1：用户自定义
     */
    private int validDateType;

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
