package org.llj.shortlink.admin.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.llj.shortlink.admin.dao.entity.UserDo;
import org.llj.shortlink.admin.dto.req.UserLoginReqDTO;
import org.llj.shortlink.admin.dto.req.UserRegisterReqDTO;
import org.llj.shortlink.admin.dto.req.UserUpdatereqDTO;
import org.llj.shortlink.admin.dto.resp.UserDTO;
import org.llj.shortlink.admin.dto.resp.UserLoginRespDTO;


public interface UserService extends IService<UserDo> {
    UserDTO getUserByUserName(String username);
    Boolean checkUserNameIsExist(String username);
    void registerUser(UserRegisterReqDTO userRegisterReqDTO);

    void updateUser(UserUpdatereqDTO updatereqDTO);

    UserLoginRespDTO loginUser(UserLoginReqDTO userLoginReqDTO);

    void logout(String username);
}
