package org.llj.shortlink.project.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import groovy.util.logging.Slf4j;
import lombok.RequiredArgsConstructor;
import org.llj.shortlink.project.common.Exception.ServiceException;
import org.llj.shortlink.project.dao.entity.ShortLinkDO;
import org.llj.shortlink.project.dao.mapper.ShortLinkMapper;
import org.llj.shortlink.project.dto.req.LinkCreateReqDTO;
import org.llj.shortlink.project.dto.req.ShortLinkPageReqDTO;
import org.llj.shortlink.project.dto.resp.LinkCreateRespDTO;
import org.llj.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import org.llj.shortlink.project.service.ShortLinkService;
import org.llj.shortlink.project.utils.HashUtil;
import org.redisson.api.RBloomFilter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.rmi.server.ServerCloneException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {
    private  final RBloomFilter<String> rBloomFilter;
    @Override
    public LinkCreateRespDTO createShortLink(LinkCreateReqDTO linkCreateReqDTO) {
        String shortLinkSuffix = generateLinkSuffix(linkCreateReqDTO);
        String fullShortLinkUrl = linkCreateReqDTO.getDomain() + '/' + shortLinkSuffix;
        LambdaQueryWrapper<ShortLinkDO> QueryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, fullShortLinkUrl);
        ShortLinkDO linkDO = baseMapper.selectOne(QueryWrapper);
        if(linkDO != null) throw new ServiceException("当前短连接已存在");
        rBloomFilter.add(fullShortLinkUrl);
        ShortLinkDO shortLinkDO =  ShortLinkDO.builder()
                .domain(linkCreateReqDTO.getDomain())
                .shortUri(shortLinkSuffix)
                .originUrl(linkCreateReqDTO.getOriginUrl())
                .fullShortUrl(fullShortLinkUrl)
                .gid(linkCreateReqDTO.getGid())
                .describe(linkCreateReqDTO.getDescribe())
                .createdType(linkCreateReqDTO.getCreatedType())
                .validDateType(linkCreateReqDTO.getValidDateType())
                .clickNum(0)
                .build();
        try{
            baseMapper.insert(shortLinkDO);
        }catch (DuplicateKeyException exception){
            log.warn("短链接:"+fullShortLinkUrl+" 重复入库");
            throw  new ServiceException("短链接重复");
        }
        return LinkCreateRespDTO.builder()
                .fullShortUrl(shortLinkDO.getFullShortUrl())
                .originUrl(shortLinkDO.getOriginUrl())
                .gid(shortLinkDO.getGid())
                .clickNum(shortLinkDO.getClickNum())
                .build();
    }

    @Override
    public IPage<ShortLinkPageRespDTO> getPage(ShortLinkPageReqDTO shortLinkPageReqDTO) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class).eq(ShortLinkDO::getGid, shortLinkPageReqDTO.getGid())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0)
                .orderByDesc(ShortLinkDO::getCreateTime);
        IPage<ShortLinkDO> pageResults = baseMapper.selectPage(shortLinkPageReqDTO, queryWrapper);
        return pageResults.convert(res -> BeanUtil.toBean(res, ShortLinkPageRespDTO.class));
    }

    public  String generateLinkSuffix(LinkCreateReqDTO linkCreateReqDTO){
        int cnt = 0;
        int maxTryCount = 10;
        String originUrl = linkCreateReqDTO.getOriginUrl();
        String shortLinkSuffix = null;
        while (true) {
            if(cnt >= maxTryCount) try {
                throw  new ServerCloneException("操作频繁，请稍后再试");
            } catch (ServerCloneException e) {
                throw new RuntimeException(e);
            }
            originUrl += System.currentTimeMillis();
            shortLinkSuffix = HashUtil.hashToBase62(originUrl);
            String fullShortLinkUrl = linkCreateReqDTO.getDomain() + '/' + shortLinkSuffix;
            if(!rBloomFilter.contains(fullShortLinkUrl)){
                break;
            }
            cnt++;
        }

        return shortLinkSuffix;
    }
}
