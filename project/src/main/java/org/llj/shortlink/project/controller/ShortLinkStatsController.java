package org.llj.shortlink.project.controller;

import lombok.RequiredArgsConstructor;
import org.llj.shortlink.project.common.convention.result.Result;
import org.llj.shortlink.project.common.convention.result.Results;
import org.llj.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import org.llj.shortlink.project.dto.resp.ShortLinkStatsRespDTO;
import org.llj.shortlink.project.service.ShortLinkStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ShortLinkStatsController {
    private final ShortLinkStatsService shortLinkStatsService;
    /**
     * 访问单个短链接指定时间内监控数据
     */
    @GetMapping("/api/shortlink/v1/stats")
    public Result<ShortLinkStatsRespDTO> shortLinkStats(ShortLinkStatsReqDTO requestParam) {
        return Results.success(shortLinkStatsService.oneShortLinkStats(requestParam));
    }
}
