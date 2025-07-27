package org.llj.shortlink.admin.service.impl;

import org.springframework.stereotype.Service;

@Service
public class remoteService {
    /**
     * 批量创建短链接
     *
     * @param requestParam 批量创建短链接请求参数
     * @return 短链接批量创建响应
     */
//    default Result<ShortLinkBatchCreateRespDTO> batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam) {
//        String resultBodyStr = HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/create/batch", JSON.toJSONString(requestParam));
//        return JSON.parseObject(resultBodyStr, new TypeReference<>() {
//        });
//    }
}
