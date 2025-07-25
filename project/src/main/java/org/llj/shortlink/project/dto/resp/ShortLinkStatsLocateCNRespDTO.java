package org.llj.shortlink.project.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 短链接地区监控响应参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortLinkStatsLocateCNRespDTO {
    /**
     * 统计
     */
    private Integer cnt;

    /**
     * 地区
     */
    private String locate;

    /**
     * 占比
     */
    private Double ratio;
}
