package org.llj.shortlink.admin.remote.dto.req;

import lombok.Data;

@Data
public class LinkCreateReqDTO {
    /**
     * 域名
     */
    private String domain;



    /**
     * 原始链接
     */
    private String originUrl;


    /**
     * 分组标识
     */
    private String gid;


    /**
     * 创建类型 0：控制台 1：接口
     */
    private int createdType;

    /**
     * 有效期类型 0：永久有效 1：用户自定义
     */
    private int validDateType;


    /**
     * 描述
     */
    private String describe;
}
