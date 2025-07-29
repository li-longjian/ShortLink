package org.llj.shortlink.project.controller;

import lombok.RequiredArgsConstructor;
import org.llj.shortlink.project.common.convention.result.Result;
import org.llj.shortlink.project.common.convention.result.Results;
import org.llj.shortlink.project.service.GetTitleByUrlService;
import org.llj.shortlink.project.service.ShortLinkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GetTitleController {
    private final ShortLinkService shortLinkService;
    private final GetTitleByUrlService getTitleByUrlService;

    /**
     * 获取原链接标题
     * @param url
     * @return
     */
    @GetMapping("/api/shortlink/v1/title")
    public Result<String> getTitle(@RequestParam("url") String url) {
        return Results.success(getTitleByUrlService.getTitle(url));
    }
}
