package org.llj.shortlink.admin.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.llj.shortlink.admin.common.Exception.ClientException;
import org.llj.shortlink.admin.common.context.BaseContext;
import org.llj.shortlink.admin.common.convention.BaseErrorCode;
import org.llj.shortlink.admin.dao.entity.UserDo;
import org.llj.shortlink.admin.dao.mapper.UserMapper;
import org.llj.shortlink.admin.dto.req.UserLoginReqDTO;
import org.llj.shortlink.admin.dto.req.UserRegisterReqDTO;
import org.llj.shortlink.admin.dto.req.UserUpdatereqDTO;
import org.llj.shortlink.admin.dto.resp.UserDTO;
import org.llj.shortlink.admin.dto.resp.UserLoginRespDTO;
import org.llj.shortlink.admin.service.GroupService;
import org.llj.shortlink.admin.service.UserService;
import org.llj.shortlink.admin.utils.JwtUtil;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.llj.shortlink.admin.common.constant.RedissionCacheConstant.LOCK_USER_REGISTER_KEY;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDo> implements UserService{
    private  final RBloomFilter<String> rBloomFilter;
    private  final RedissonClient redissonClient;
    private static final String TOKEN_PREFIX = "shortlink:token:";
    private final StringRedisTemplate redisTemplate;
    private  final String SECRET_KEY = "shortlink-token-secretKey-shortlink-token-secretKey-shortlink-token-secretKey-shortlink-token-secretKey-shortlink-token-secretKey-shortlink-token-secretKey";
    private final GroupService groupService;

    @Override
    public UserDTO getUserByUserName(String username) {
        LambdaQueryWrapper<UserDo> queryWrapper = Wrappers.lambdaQuery(UserDo.class)
                .eq(UserDo::getUsername, username);
        UserDo userDo = baseMapper.selectOne(queryWrapper);
        if(userDo == null) throw new ClientException("用户名不存在", BaseErrorCode.USER_NOT_EXIST);
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(userDo, userDTO);
        return userDTO;
    }

    /**
     * 判断用户名是否存在,使用Redission布隆过滤器防止缓存穿透 ，布隆过滤器判断不存在，则一定不存在，判定存在存在概率误判
     * @param username
     * @return
     */
    @Override
    public Boolean checkUserNameIsExist(String username) {
       return rBloomFilter.contains(username);
    }

    /**
     * 用户注册
     * @param userRegisterReqDTO
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void registerUser(UserRegisterReqDTO userRegisterReqDTO) {
        String USERNAME = userRegisterReqDTO.getUsername();
        //首先通过布隆过滤器快速判断是否存在相同用户名
        if(checkUserNameIsExist(USERNAME)) throw  new ClientException(BaseErrorCode.USER_NAME_EXIST_ERROR);
        //对用户名进行加分布式锁: 防止恶意用户对某一未使用用户名大量请求
        RLock lock = redissonClient.getLock(LOCK_USER_REGISTER_KEY + USERNAME);
        //布隆过滤器误判或高并发情况下没有拿到锁说明已经被占用了
        if(!lock.tryLock()) throw  new ClientException(BaseErrorCode.USER_NAME_EXIST_ERROR);
        try {
            int inserted = baseMapper.insert(BeanUtil.toBean(userRegisterReqDTO, UserDo.class));
            if (inserted < 1) throw new ClientException(BaseErrorCode.USER_REGISTER_ERROR);
            //将用户注册成功的用户名加到布隆过滤器中,防止缓存穿透
            rBloomFilter.add(USERNAME);
            //为新注册用户添加默认分组
            groupService.addGroup("default",USERNAME);
        }catch (DuplicateKeyException e){
            throw new ClientException(BaseErrorCode.USER_NAME_EXIST_ERROR);
        }finally {
            lock.unlock();
        }
    }

    /**
     * 更新用户信息
     * @param updatereqDTO
     */
    @Override
    public void updateUser(UserUpdatereqDTO updatereqDTO) {
        if(!Objects.equals(BaseContext.getUserName(),updatereqDTO.getUsername())){
            throw new ClientException("当前请求参数非法");
        }
        LambdaUpdateWrapper<UserDo> wrapper = Wrappers.lambdaUpdate(UserDo.class).eq(UserDo::getUsername, updatereqDTO.getUsername());
        baseMapper.update(BeanUtil.toBean(updatereqDTO,UserDo.class), wrapper);
    }

    /**
     * 用户登录
     * @param userLoginReqDTO
     * @return
     */
    @Override
    public UserLoginRespDTO loginUser(UserLoginReqDTO userLoginReqDTO) {
        LambdaQueryWrapper<UserDo> queryWrapper = Wrappers.lambdaQuery(UserDo.class)
                .eq(UserDo::getUsername, userLoginReqDTO.getUsername())
                .eq(UserDo::getPassword, userLoginReqDTO.getPassword())
                .eq(UserDo::getDelFlag,0);
        UserDo userDo = baseMapper.selectOne(queryWrapper);
        if(userDo == null) throw new ClientException("用户名或密码错误");

        String pre_token  = BaseContext.getUserName();
        if( pre_token != null && !pre_token.isEmpty()) return new UserLoginRespDTO(pre_token); //保证多端登录
        //用户名密码正确，生成token
        Map<String,Object> claims = new HashMap<>();
        claims.put("username",userDo.getUsername());
        String token = JwtUtil.createJWT( SECRET_KEY,1000*60*60*24*7, claims);


        return new UserLoginRespDTO(token);
        //redis: shortlink:token:username: xxxxxxxxxxx
        //String tokenKey = TOKEN_PREFIX + userDo.getUsername();
//        if(redisTemplate.hasKey(tokenKey)) throw new ClientException("用户已登录");
//
//        String token = UUID.randomUUID().toString().replace("-", "");
//        redisTemplate.opsForValue().set(tokenKey,token,7, TimeUnit.DAYS);
//        return new UserLoginRespDTO(token);
    }
    public boolean checkLogin(String username){
        return redisTemplate.hasKey(TOKEN_PREFIX + username);
    }
    @Override
    public void logout(String username) {
        String userName = BaseContext.getUserName();
        if(userName == null || userName.isEmpty()) throw new ClientException("用户未登录");
        BaseContext.clearUserName();
//        if(checkLogin(username)){
//            redisTemplate.delete(TOKEN_PREFIX + username);
//            return;
//        }
//        throw new ClientException("用户未登录");
    }
}
