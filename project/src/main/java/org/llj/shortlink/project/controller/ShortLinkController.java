package org.llj.shortlink.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.llj.shortlink.project.common.convention.result.Result;
import org.llj.shortlink.project.common.convention.result.Results;
import org.llj.shortlink.project.dto.req.LinkCreateReqDTO;
import org.llj.shortlink.project.dto.req.ShortLinkPageReqDTO;
import org.llj.shortlink.project.dto.resp.LinkCreateRespDTO;
import org.llj.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import org.llj.shortlink.project.service.ShortLinkService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shortlink/v1/link")
@RequiredArgsConstructor
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    @PostMapping("/create")
    public Result<LinkCreateRespDTO> createShortLink(@RequestBody LinkCreateReqDTO linkCreateReqDTO) {
        return Results.success(shortLinkService.createShortLink(linkCreateReqDTO));
    }

    /**
     * 以分页查询某个分组下的所有有效的短连接
     * @param shortLinkPageReqDTO
     * @return
     */
    @GetMapping("/page")
    public Result<IPage<ShortLinkPageRespDTO>> getShortLinkPage(ShortLinkPageReqDTO shortLinkPageReqDTO) {
        return Results.success(shortLinkService.getPage(shortLinkPageReqDTO));

    }

}
