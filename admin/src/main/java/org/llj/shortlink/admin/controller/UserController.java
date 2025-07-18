package org.llj.shortlink.admin.controller;

import lombok.RequiredArgsConstructor;
import org.llj.shortlink.admin.common.convention.result.Result;
import org.llj.shortlink.admin.common.convention.result.Results;
import org.llj.shortlink.admin.dto.req.UserLoginReqDTO;
import org.llj.shortlink.admin.dto.req.UserRegisterReqDTO;
import org.llj.shortlink.admin.dto.req.UserUpdatereqDTO;
import org.llj.shortlink.admin.dto.resp.UserDTO;
import org.llj.shortlink.admin.dto.resp.UserLoginRespDTO;
import org.llj.shortlink.admin.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shortlink/admin/v1/user")
@RequiredArgsConstructor
public class UserController {
    public final UserService userService;

    /**
     * 根据用户名查找用户
     * @param username
     * @return
     */
    @GetMapping("/{username}")
    public Result<UserDTO> getUserByUserName(@PathVariable("username") String username) {
        UserDTO name = userService.getUserByUserName(username);

        return Results.success(name);
    }

    /**
     * 检查用户名是否已经存在
     * @param username
     * @return
     */
    @GetMapping("/check-name/{username}")
    public Result<Boolean> checkUserName(@PathVariable("username") String username){
        return Results.success(userService.checkUserNameIsExist(username));
    }

    /**
     * 注册用户
     * @param userRegisterReqDTO
     * @return
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody UserRegisterReqDTO userRegisterReqDTO) {
        userService.registerUser(userRegisterReqDTO);
        return Results.success();
    }

    /**
     * 更新用户信息
     * @param updatereqDTO
     * @return
     */
    @PutMapping
    public Result<Void> update(@RequestBody UserUpdatereqDTO updatereqDTO) {
        userService.updateUser(updatereqDTO);
        return Results.success();
    }

    /**
     * 用户登录
     * @param userLoginReqDTO
     * @return
     */
    @PostMapping("/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO userLoginReqDTO) {
        return Results.success(userService.loginUser(userLoginReqDTO));
    }

    /**
     * 退出登录
     * @param username
     * @return
     */
    @DeleteMapping("/logout")
    public Result<Void> logout(@RequestParam("username") String username) {
        userService.logout(username);
        return Results.success();
    }
}
