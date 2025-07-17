package org.llj.shortlink.admin.remote.dto;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.llj.shortlink.admin.common.convention.result.Result;
import org.llj.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import org.llj.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;

import java.util.HashMap;
import java.util.Map;


/**
 * 短连接中台远程服务调用
 */
public interface RemoteService {

    default Result<IPage<ShortLinkPageRespDTO>> getShortLinkPage(ShortLinkPageReqDTO shortLinkPageReqDTO) {

        Map<String,Object> requestParamMap = new HashMap<>();
        requestParamMap.put("gid", shortLinkPageReqDTO.getGid());
        requestParamMap.put("current", shortLinkPageReqDTO.getCurrent());
        requestParamMap.put("size", shortLinkPageReqDTO.getSize());

        String resultString = HttpUtil.get("http://127.0.0.1:8001/api/shortlink/v1/link/page", requestParamMap);
        return JSON.parseObject(resultString,new TypeReference<Result<IPage<ShortLinkPageRespDTO>>>(){});
    }

}
