package org.llj.shortlink.project.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import org.llj.shortlink.project.dao.entity.LinkAccessLogDO;

/**
 * 监控访问记录 请求参数
 */
@Data
public class LinkAccessRecodeReqDTO extends Page<LinkAccessLogDO> {
    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 开始日期
     */
    private String startDate;

    /**
     * 结束日期
     */
    private String endDate;
}
