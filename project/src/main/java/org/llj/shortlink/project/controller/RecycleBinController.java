package org.llj.shortlink.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.llj.shortlink.project.common.convention.result.Result;
import org.llj.shortlink.project.common.convention.result.Results;
import org.llj.shortlink.project.dto.req.RecycleBinAddReqDTO;
import org.llj.shortlink.project.dto.req.RecycleBinPageReqDTO;
import org.llj.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import org.llj.shortlink.project.service.RecycleBinService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RecycleBinController {
    private final RecycleBinService recycleBinService;

    /**
     * 短连接回收到回收站
     * @param recycleBinAddReqDTO
     * @return
     */
    @PostMapping("/api/shortlink/v1/recycle-bin/add")
    public Result<Void> addLinkToRecycleBin(@RequestBody RecycleBinAddReqDTO recycleBinAddReqDTO) {
        recycleBinService.addToRecycleBin(recycleBinAddReqDTO);
        return Results.success();
    }

    /**
     * 分页查询回收站所有有效的短连接
     *
     * @return
     */
    @GetMapping("/api/shortlink/v1/recycle-bin/page")
    public Result<IPage<ShortLinkPageRespDTO>> getShortLinkPage(RecycleBinPageReqDTO recycleBinPageReqDTO) {
        return Results.success(recycleBinService.getPage(recycleBinPageReqDTO));

    }
}
