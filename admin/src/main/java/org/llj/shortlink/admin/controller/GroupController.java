package org.llj.shortlink.admin.controller;

import lombok.RequiredArgsConstructor;
import org.llj.shortlink.admin.common.convention.result.Result;
import org.llj.shortlink.admin.common.convention.result.Results;
import org.llj.shortlink.admin.dto.req.GroupAddReqDTO;
import org.llj.shortlink.admin.dto.req.GroupOrderReqDTO;
import org.llj.shortlink.admin.dto.req.GroupUpdateReqDTO;
import org.llj.shortlink.admin.dto.resp.GroupGetRespDTO;
import org.llj.shortlink.admin.service.GroupService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shortlink/admin/v1/group")
@RequiredArgsConstructor
public class GroupController {
     private  final GroupService groupService;

    /**
     * 新增分组
     * @param groupAddReqDTO
     * @return
     */
     @PostMapping
     public Result<Void> addGroup(@RequestBody GroupAddReqDTO groupAddReqDTO) {
         groupService.addGroup(groupAddReqDTO.getName());
         return Results.success();
     }

    /**
     * 获取分组
     * @return
     */
     @GetMapping
    public Result<List<GroupGetRespDTO>> getAllGroups() {
         List<GroupGetRespDTO> groupList = groupService.groupList();
         return  Results.success(groupList);
     }

    /**
     * 更新分组
     * @param groupUpdateReqDTO
     * @return
     */
     @PutMapping
     public Result<Void> updateGroup(@RequestBody GroupUpdateReqDTO groupUpdateReqDTO) {
         groupService.updateGroup(groupUpdateReqDTO);
         return Results.success();
     }

    /**
     * 删除分组
     * @param gid
     * @return
     */
    @DeleteMapping
    public Result<Void> deleteGroup(@RequestParam("gid") String gid) {
        groupService.deleteGroup(gid);
        return Results.success();
    }

    @PostMapping("/order")
    public Result<Void> updateGroupOdered(@RequestBody List<GroupOrderReqDTO> orderList){
        groupService.updateOrdered(orderList);
        return Results.success();
    }
}
