package org.llj.shortlink.admin.remote.dto.req;

import lombok.Data;

@Data
public class RecycleBinRemoveReqDTO {
    private String gid;
    private String fullShortUrl;
}
