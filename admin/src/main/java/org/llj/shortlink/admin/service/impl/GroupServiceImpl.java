package org.llj.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.val;
import org.llj.shortlink.admin.common.Exception.ClientException;
import org.llj.shortlink.admin.common.context.BaseContext;
import org.llj.shortlink.admin.dao.entity.GroupDO;
import org.llj.shortlink.admin.dao.mapper.GroupMapper;
import org.llj.shortlink.admin.dto.req.GroupOrderReqDTO;
import org.llj.shortlink.admin.dto.req.GroupUpdateReqDTO;
import org.llj.shortlink.admin.dto.resp.GroupGetRespDTO;
import org.llj.shortlink.admin.service.GroupService;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.llj.shortlink.admin.utils.RandomStringGenerator.generateSixDigits;

@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {

    //private static final String USERNAME = BaseContext.getUserName();
    @Override
    public void addGroup(String name) {
        val queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getName, name)
                .eq(GroupDO::getUsername, BaseContext.getUserName())
                .eq(GroupDO::getDelFlag, 0);
        GroupDO groupDO = baseMapper.selectOne(queryWrapper);
        if(groupDO != null) throw  new ClientException("当前组名已经存在");
        String gid = "";
        do{
             gid = generateSixDigits();
        }while(checkGIDisExist(gid));
        GroupDO build = GroupDO.builder()
                .name(name)
                .gid(gid)
                .build();
        baseMapper.insert(build);
    }

    /**
     * 获取所有有效分组
     * @return
     */
    @Override
    public List<GroupGetRespDTO> groupList() {

        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getUsername,BaseContext.getUserName())
                .orderByDesc(GroupDO::getOrdered, GroupDO::getUpdateTime)
                .eq(GroupDO::getDelFlag, 0);
        List<GroupDO> groupDOList = baseMapper.selectList(queryWrapper);

        return BeanUtil.copyToList(groupDOList, GroupGetRespDTO.class);
    }

    @Override
    public void updateGroup(GroupUpdateReqDTO groupUpdateReqDTO) {
        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getGid, groupUpdateReqDTO.getGid())
                .eq(GroupDO::getUsername, BaseContext.getUserName())
                .eq(GroupDO::getDelFlag, 0);
//        GroupDO groupDO = GroupDO.builder()
//                .name(groupUpdateReqDTO.getName())
//                .build();
       // System.out.println("username:"+BaseContext.getUserName()+"gid"+groupUpdateReqDTO.getGid());
        GroupDO groupDO = new GroupDO();
        groupDO.setName(groupUpdateReqDTO.getName());
        baseMapper.update(groupDO, updateWrapper);
    }

    @Override
    public void deleteGroup(String gid) {
        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getUsername, BaseContext.getUserName())
                .eq(GroupDO::getDelFlag, 0);

        GroupDO groupDO = new GroupDO();
        groupDO.setDelFlag(1);
        baseMapper.update(groupDO, updateWrapper);
    }

    @Override
    public void updateOrdered(List<GroupOrderReqDTO> orderList) {
        orderList.forEach(it -> {
            LambdaUpdateWrapper<GroupDO> wrapper = Wrappers.lambdaUpdate(GroupDO.class)
                    .eq(GroupDO::getGid, it.getGid())
                    .eq(GroupDO::getUsername, BaseContext.getUserName())
                    .eq(GroupDO::getDelFlag, 0);
            GroupDO build = GroupDO.builder()
                    .ordered(it.getOrder())
                    .build();
            baseMapper.update(build,wrapper);

        });
    }


    public boolean checkGIDisExist(String gid) {
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class).eq(GroupDO::getGid, gid);
        GroupDO groupDO = baseMapper.selectOne(queryWrapper);
        return groupDO != null;
    }
}
