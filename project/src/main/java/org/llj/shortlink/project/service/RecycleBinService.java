package org.llj.shortlink.project.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.llj.shortlink.project.dao.entity.ShortLinkDO;
import org.llj.shortlink.project.dto.req.RecycleBinAddReqDTO;

public interface RecycleBinService extends IService<ShortLinkDO> {
    void addToRecycleBin(RecycleBinAddReqDTO recycleBinAddReqDTO);
}
