package org.llj.shortlink.project.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.llj.shortlink.project.dao.entity.ShortLinkDO;
import org.llj.shortlink.project.dto.req.LinkCreateReqDTO;
import org.llj.shortlink.project.dto.resp.LinkCreateRespDTO;

public interface ShortLinkService extends IService<ShortLinkDO> {
    LinkCreateRespDTO createShortLink(LinkCreateReqDTO linkCreateReqDTO);
}
