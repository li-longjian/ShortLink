package org.llj.shortlink.admin.remote.dto.req;

import lombok.Data;

@Data
public class RecycleBinPageReqDTO {
    private long size;
    private long current;
}
