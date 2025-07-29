package org.llj.shortlink.admin.remote.dto.resp;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
     *
     */
    private String locate;

    /**
     * 占比
     */
    private Double ratio;
}
