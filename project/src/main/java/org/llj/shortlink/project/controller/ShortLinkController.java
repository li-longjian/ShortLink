package org.llj.shortlink.project.controller;

import lombok.RequiredArgsConstructor;
import org.llj.shortlink.project.common.convention.result.Result;
import org.llj.shortlink.project.common.convention.result.Results;
import org.llj.shortlink.project.dto.req.LinkCreateReqDTO;
import org.llj.shortlink.project.dto.resp.LinkCreateRespDTO;
import org.llj.shortlink.project.service.ShortLinkService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shortlink/v1/link")
@RequiredArgsConstructor
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    @PostMapping("/create")
    public Result<LinkCreateRespDTO> createShortLink(@RequestBody LinkCreateReqDTO linkCreateReqDTO) {
        return Results.success(shortLinkService.createShortLink(linkCreateReqDTO));
    }

}
