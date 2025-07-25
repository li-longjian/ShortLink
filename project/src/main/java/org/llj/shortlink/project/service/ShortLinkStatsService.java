package org.llj.shortlink.project.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import org.llj.shortlink.project.dto.req.LinkAccessRecodeReqDTO;
import org.llj.shortlink.project.dto.req.LinkStatsGroupReqDTO;
import org.llj.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import org.llj.shortlink.project.dto.resp.LinkAccessRecodeRespDTO;
import org.llj.shortlink.project.dto.resp.ShortLinkStatsRespDTO;

public interface ShortLinkStatsService {
    /**
     * 获取单个短链接监控数据
     *
     * @param requestParam 获取短链接监控数据入参
     * @return 短链接监控数据
     */
    ShortLinkStatsRespDTO oneShortLinkStats(ShortLinkStatsReqDTO requestParam);

    IPage<LinkAccessRecodeRespDTO> linkAccessStatsPage(LinkAccessRecodeReqDTO requestParam);

    ShortLinkStatsRespDTO groupShortLinkStats(LinkStatsGroupReqDTO requestParam);
}
