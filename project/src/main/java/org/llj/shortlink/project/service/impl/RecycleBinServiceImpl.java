package org.llj.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.llj.shortlink.project.common.Exception.ServiceException;
import org.llj.shortlink.project.common.context.BaseContext;
import org.llj.shortlink.project.dao.entity.GroupDO;
import org.llj.shortlink.project.dao.entity.ShortLinkDO;
import org.llj.shortlink.project.dao.mapper.GroupMapper;
import org.llj.shortlink.project.dao.mapper.RecycleBinMapper;
import org.llj.shortlink.project.dto.req.RecycleBinAddReqDTO;
import org.llj.shortlink.project.dto.req.RecycleBinDeleteReqDTO;
import org.llj.shortlink.project.dto.req.RecycleBinPageReqDTO;
import org.llj.shortlink.project.dto.req.RecycleBinRecoverReqDTO;
import org.llj.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import org.llj.shortlink.project.service.RecycleBinService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static org.llj.shortlink.project.common.constant.RedisKey.FULL_SHORT_URL_KEY;
import static org.llj.shortlink.project.common.constant.RedisKey.IS_NULL_FULL_SHORT_URL_KEY;

@Service
@RequiredArgsConstructor
public class RecycleBinServiceImpl extends ServiceImpl<RecycleBinMapper, ShortLinkDO> implements RecycleBinService {
    private final StringRedisTemplate stringRedisTemplate;
    private final GroupMapper groupMapper;
    /**
     * 短连接回收到回收站
     * @param recycleBinAddReqDTO
     */
    @Override
    public void addToRecycleBin(RecycleBinAddReqDTO recycleBinAddReqDTO) {
        val updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, recycleBinAddReqDTO.getGid())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getFullShortUrl, recycleBinAddReqDTO.getFullShortUrl())
                .eq(ShortLinkDO::getEnableStatus, 0);
        ShortLinkDO build = ShortLinkDO.builder()
                .enableStatus(1)//设置短连接关闭状态 0：开启， 1：关闭
                .build();
        baseMapper.update(build,updateWrapper);
        stringRedisTemplate.delete(
                String.format(FULL_SHORT_URL_KEY, recycleBinAddReqDTO.getFullShortUrl())
        );
    }

    @Override
    public IPage<ShortLinkPageRespDTO> getPage(RecycleBinPageReqDTO recycleBinPageReqDTO) {
        /**
         * 查询当前用户所有分组
         *
         */
        LambdaQueryWrapper<GroupDO> query = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getUsername, BaseContext.getUserName())
                .eq(GroupDO::getDelFlag, 0);
        List<GroupDO> groupList = groupMapper.selectList(query);
        if(groupList == null || groupList.isEmpty()) throw  new ServiceException("当前用户不存在分组");

        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .in(ShortLinkDO::getGid, groupList.stream().map(GroupDO::getGid).collect(Collectors.toList()))
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 1)
                .orderByDesc(ShortLinkDO::getUpdateTime);
        Page<ShortLinkDO> page = new Page<>(recycleBinPageReqDTO.getCurrent(), recycleBinPageReqDTO.getSize());
        IPage<ShortLinkDO> pageResults = baseMapper.selectPage(page, queryWrapper);
        return pageResults.convert(res -> BeanUtil.toBean(res, ShortLinkPageRespDTO.class));
    }

    @Override
    public void recoverShortLink(RecycleBinRecoverReqDTO recycleBinRecoverReqDTO) {
        val updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, recycleBinRecoverReqDTO.getGid())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getFullShortUrl, recycleBinRecoverReqDTO.getFullShortUrl())
                .eq(ShortLinkDO::getEnableStatus, 1);
        ShortLinkDO build = ShortLinkDO.builder()
                .enableStatus(0)//设置短连接关闭状态 0：开启， 1：关闭
                .build();
        baseMapper.update(build,updateWrapper);
        stringRedisTemplate.delete(String.format(IS_NULL_FULL_SHORT_URL_KEY, recycleBinRecoverReqDTO.getFullShortUrl()));
    }

    @Override
    public void deleteShortLink(RecycleBinDeleteReqDTO recycleBinDeleteReqDTO) {
        val updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, recycleBinDeleteReqDTO.getGid())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getFullShortUrl, recycleBinDeleteReqDTO.getFullShortUrl())
                .eq(ShortLinkDO::getEnableStatus, 1);
        baseMapper.delete(updateWrapper);//配置了mp 的逻辑删除功能，并不是物理删除
    }
}
