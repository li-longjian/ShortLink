package org.llj.shortlink.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.llj.shortlink.project.common.convention.result.Result;
import org.llj.shortlink.project.common.convention.result.Results;
import org.llj.shortlink.project.dto.req.LinkAccessRecodeReqDTO;
import org.llj.shortlink.project.dto.req.LinkStatsGroupReqDTO;
import org.llj.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import org.llj.shortlink.project.dto.resp.LinkAccessRecodeRespDTO;
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

    /**
     * 访问短链接分组指定时间内监控数据
     */
    @GetMapping("/api/shortlink/v1/stats/group")
    public Result<ShortLinkStatsRespDTO> shortLinkGroupStats(LinkStatsGroupReqDTO requestParam) {
        return Results.success(shortLinkStatsService.groupShortLinkStats(requestParam));
    }

    /**
     * 指定时间内，访问记录数据
     * @param requestParam
     * @return
     */
    @GetMapping("/api/shortlink/v1/stats-recode")
    public Result<IPage<LinkAccessRecodeRespDTO>> shortLinkAccessRecodePage(LinkAccessRecodeReqDTO requestParam) {
        return Results.success(shortLinkStatsService.linkAccessStatsPage(requestParam));
    }
}
