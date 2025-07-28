package org.llj.shortlink.project.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.llj.shortlink.project.dao.entity.ShortLinkDO;
import org.llj.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import org.llj.shortlink.project.dto.req.*;
import org.llj.shortlink.project.dto.resp.GroupLinkCountRespDTO;
import org.llj.shortlink.project.dto.resp.LinkCreateRespDTO;
import org.llj.shortlink.project.dto.resp.ShortLinkBatchCreateRespDTO;
import org.llj.shortlink.project.dto.resp.ShortLinkPageRespDTO;

import java.util.List;

public interface ShortLinkService extends IService<ShortLinkDO> {
    LinkCreateRespDTO createShortLink(LinkCreateReqDTO linkCreateReqDTO);

    IPage<ShortLinkPageRespDTO> getPage(ShortLinkPageReqDTO shortLinkPageReqDTO);

    List<GroupLinkCountRespDTO> getGroupLinkCount(List<String> requestParam);

    void updateLink(LinkUpdateReqDTO linkUpdateReqDTO);

    void reStoreUrl(String shortUri, HttpServletRequest request, HttpServletResponse response);
    /**
     * 批量创建短链接
     *
     * @param requestParam 批量创建短链接请求参数
     * @return 批量创建短链接返回参数
     */
    ShortLinkBatchCreateRespDTO batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam);

    void shortLinkStats(String fullShortUrl, String gid, ShortLinkStatsRecordDTO shortLinkStatsRecordDTO);
}
