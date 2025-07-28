package org.llj.shortlink.project.common.handler;


import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.llj.shortlink.project.common.convention.result.Result;
import org.llj.shortlink.project.dto.req.LinkCreateReqDTO;
import org.llj.shortlink.project.dto.resp.LinkCreateRespDTO;

public class CustomBlockHandler {
    public static Result<LinkCreateRespDTO> createShortLinkBlockHandlerMethod(LinkCreateReqDTO requestParam, BlockException exception) {
        return new Result<LinkCreateRespDTO>().setCode("B100000").setMessage("当前访问网站人数过多，请稍后再试...");
    }
}
