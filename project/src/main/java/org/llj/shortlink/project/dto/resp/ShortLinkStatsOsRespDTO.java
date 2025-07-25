package org.llj.shortlink.project.dto.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
/**
 * 短链接操作系统监控响应参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortLinkStatsOsRespDTO {

    /**
     * 统计
     */
    private Integer cnt;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 占比
     */
    private Double ratio;
}
