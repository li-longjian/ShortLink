package org.llj.shortlink.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.llj.shortlink.project.common.convention.result.Result;
import org.llj.shortlink.project.common.convention.result.Results;
import org.llj.shortlink.project.dto.req.RecycleBinAddReqDTO;
import org.llj.shortlink.project.dto.req.RecycleBinDeleteReqDTO;
import org.llj.shortlink.project.dto.req.RecycleBinPageReqDTO;
import org.llj.shortlink.project.dto.req.RecycleBinRecoverReqDTO;
import org.llj.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import org.llj.shortlink.project.service.RecycleBinService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shortlink/v1/recycle-bin")
public class RecycleBinController {
    private final RecycleBinService recycleBinService;

    /**
     * 短连接回收到回收站
     * @param recycleBinAddReqDTO
     * @return
     */
    @PostMapping("/add")
    public Result<Void> addLinkToRecycleBin(@RequestBody RecycleBinAddReqDTO recycleBinAddReqDTO) {
        recycleBinService.addToRecycleBin(recycleBinAddReqDTO);
        return Results.success();
    }

    /**
     * 恢复回收站短连接
     * @param recycleBinRecoverReqDTO
     * @return
     */
    @PostMapping("/recover")
    public Result<Void> RecoverShortLink(@RequestBody RecycleBinRecoverReqDTO recycleBinRecoverReqDTO) {
        recycleBinService.recoverShortLink(recycleBinRecoverReqDTO);
        return Results.success();
    }

    /**
     * 删除回收站短连接
     * @param recycleBinDeleteReqDTO
     * @return
     */
    @PostMapping("/remove")
    public Result<Void> deleteShortLink(@RequestBody RecycleBinDeleteReqDTO recycleBinDeleteReqDTO) {
        recycleBinService.deleteShortLink(recycleBinDeleteReqDTO);
        return Results.success();
    }
    /**
     * 分页查询回收站所有有效的短连接
     *
     * @return
     */
    @GetMapping("/page")
    public Result<IPage<ShortLinkPageRespDTO>> getShortLinkPage(RecycleBinPageReqDTO recycleBinPageReqDTO) {
        return Results.success(recycleBinService.getPage(recycleBinPageReqDTO));

    }
}
