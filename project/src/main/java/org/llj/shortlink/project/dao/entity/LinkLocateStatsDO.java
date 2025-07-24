package org.llj.shortlink.project.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LinkLocateStatsDO {
    private Long id;
    private String fullShortUrl;
    private String gid;
    private Date date;
    private Integer cnt;
    private String province;
    private String city;
    private String adcode;
    private String country;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer delFlag;
}
