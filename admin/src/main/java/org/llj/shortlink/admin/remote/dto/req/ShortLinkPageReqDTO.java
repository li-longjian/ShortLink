package org.llj.shortlink.admin.remote.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import org.llj.shortlink.admin.remote.dto.ShortLinkDO;


@Data
public class ShortLinkPageReqDTO extends Page<ShortLinkDO> {
    private String gid;
}
