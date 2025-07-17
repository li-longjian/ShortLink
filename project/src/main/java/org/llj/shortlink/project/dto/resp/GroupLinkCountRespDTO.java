package org.llj.shortlink.project.dto.resp;

import lombok.Data;

/**
 * 统计gid下有多少链接
 */
@Data
public class GroupLinkCountRespDTO {
    private String gid;
    private  Integer shortLinkCount;
}
