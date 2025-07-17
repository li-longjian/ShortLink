package org.llj.shortlink.project.dto.resp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LinkCreateRespDTO {


    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 原始链接
     */
    private String originUrl;

    /**
     * 点击量
     */
    private Integer clickNum;

    /**
     * 分组标识
     */
    private String gid;





}
