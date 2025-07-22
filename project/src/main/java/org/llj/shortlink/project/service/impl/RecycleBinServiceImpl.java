package org.llj.shortlink.project.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.llj.shortlink.project.dao.entity.ShortLinkDO;
import org.llj.shortlink.project.dao.mapper.RecycleBinMapper;
import org.llj.shortlink.project.dto.req.RecycleBinAddReqDTO;
import org.llj.shortlink.project.service.RecycleBinService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import static org.llj.shortlink.project.common.constant.RedisKey.FULL_SHORT_URL_KEY;

@Service
@RequiredArgsConstructor
public class RecycleBinServiceImpl extends ServiceImpl<RecycleBinMapper, ShortLinkDO> implements RecycleBinService {
    private final StringRedisTemplate stringRedisTemplate;

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
}
