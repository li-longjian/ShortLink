package org.llj.shortlink.admin.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.llj.shortlink.admin.dao.entity.GroupDO;
import org.llj.shortlink.admin.dto.req.GroupOrderReqDTO;
import org.llj.shortlink.admin.dto.req.GroupUpdateReqDTO;
import org.llj.shortlink.admin.dto.resp.GroupGetRespDTO;

import java.util.List;

public interface GroupService extends IService<GroupDO> {
    void addGroup(String name);

    List<GroupGetRespDTO> groupList();

    void updateGroup(GroupUpdateReqDTO groupUpdateReqDTO);

    void deleteGroup(String gid);

    void updateOrdered(List<GroupOrderReqDTO> orderList);
}
