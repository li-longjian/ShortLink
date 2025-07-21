package org.llj.shortlink.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.llj.shortlink.project.common.convention.result.Result;
import org.llj.shortlink.project.common.convention.result.Results;
import org.llj.shortlink.project.dto.req.LinkCreateReqDTO;
import org.llj.shortlink.project.dto.req.LinkUpdateReqDTO;
import org.llj.shortlink.project.dto.req.ShortLinkPageReqDTO;
import org.llj.shortlink.project.dto.resp.GroupLinkCountRespDTO;
import org.llj.shortlink.project.dto.resp.LinkCreateRespDTO;
import org.llj.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import org.llj.shortlink.project.service.ShortLinkService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController

@RequiredArgsConstructor
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    /**
     * 创建短连接
     * @param linkCreateReqDTO
     * @return
     */
    @PostMapping("/api/shortlink/v1/link/create")
    public Result<LinkCreateRespDTO> createShortLink(@RequestBody LinkCreateReqDTO linkCreateReqDTO) {
        return Results.success(shortLinkService.createShortLink(linkCreateReqDTO));
    }

    /**
     * 更新短连接
     * @param linkUpdateReqDTO
     * @return
     */
    @PostMapping("/api/shortlink/v1/link/update")
    public Result<Void> updateShortLink(@RequestBody LinkUpdateReqDTO linkUpdateReqDTO) {
        shortLinkService.updateLink(linkUpdateReqDTO);

        return Results.success();
    }
    /**
     * 分页查询分组下的所有有效的短连接
     * @param shortLinkPageReqDTO
     * @return
     */
    @GetMapping("/api/shortlink/v1/link/page")
    public Result<IPage<ShortLinkPageRespDTO>> getShortLinkPage(ShortLinkPageReqDTO shortLinkPageReqDTO) {
        return Results.success(shortLinkService.getPage(shortLinkPageReqDTO));

    }

    /**
     * 统计分组下的短连接数量
     * @param requestParam
     * @return
     */
    @GetMapping("/api/shortlink/v1/link/count")
    public Result<List<GroupLinkCountRespDTO>> countGroupLinkCount(@RequestParam List<String> requestParam) {
        return Results.success(shortLinkService.getGroupLinkCount(requestParam));
    }

    /**
     * 短连接跳转源链接
     * @param shortUri
     * @param request
     * @param response
     * @return
     */
    @GetMapping("/{short-uri}")
    public Result<Void> reStoreUrl(@PathVariable("short-uri") String shortUri, HttpServletRequest request, HttpServletResponse response) {
        shortLinkService.reStoreUrl(shortUri,request,response);
        return Results.success();

    }
}
