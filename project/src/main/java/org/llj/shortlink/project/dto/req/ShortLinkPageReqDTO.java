package org.llj.shortlink.project.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import org.llj.shortlink.project.dao.entity.ShortLinkDO;

@Data
public class ShortLinkPageReqDTO extends Page<ShortLinkDO> {
    private String gid;
    private String orderTag;
}
