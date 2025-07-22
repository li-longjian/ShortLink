package org.llj.shortlink.project.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.llj.shortlink.project.dao.entity.ShortLinkDO;
import org.llj.shortlink.project.dto.req.RecycleBinAddReqDTO;
import org.llj.shortlink.project.dto.req.RecycleBinPageReqDTO;
import org.llj.shortlink.project.dto.req.RecycleBinRecoverReqDTO;
import org.llj.shortlink.project.dto.resp.ShortLinkPageRespDTO;

public interface RecycleBinService extends IService<ShortLinkDO> {
    void addToRecycleBin(RecycleBinAddReqDTO recycleBinAddReqDTO);

    IPage<ShortLinkPageRespDTO> getPage(RecycleBinPageReqDTO recycleBinPageReqDTO);

    void recoverShortLink(RecycleBinRecoverReqDTO recycleBinRecoverReqDTO);
}
