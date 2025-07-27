package org.llj.shortlink.admin.controller;

import lombok.RequiredArgsConstructor;
import org.llj.shortlink.admin.service.impl.remoteService;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ShortLinkController {
    private final remoteService remoteService;
    /**
     * 批量创建短链接
     */
//    @SneakyThrows
//    @PostMapping("/api/short-link/admin/v1/create/batch")
//    public void batchCreateShortLink(@RequestBody LinkBatchCreateReqDTO requestParam, HttpServletResponse response) {
//        Result<LinkBatchCreateRespDTO> shortLinkBatchCreateRespDTOResult = remoteService.batchCreateShortLink(requestParam);
//        if (shortLinkBatchCreateRespDTOResult.isSuccess()) {
//            List<LinkBaseInfoRespDTO> baseLinkInfos = shortLinkBatchCreateRespDTOResult.getData().getBaseLinkInfos();
//            EasyExcelWebUtil.write(response, "批量创建短链接-SaaS短链接系统", ShortLinkBaseInfoRespDTO.class, baseLinkInfos);
//        }
//    }
}
