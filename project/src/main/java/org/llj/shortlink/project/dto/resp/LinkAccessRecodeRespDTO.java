package org.llj.shortlink.project.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LinkAccessRecodeRespDTO {
    private String fullShortUrl;
    private String gid;
    private String browser;
    private String user;
    private String ip;
    private String os;
    private String device;
    private String locate;
    private String network;
    private String uvType; //访问类型
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:SS", timezone = "GMT+8")
    private Date createTime;
}
