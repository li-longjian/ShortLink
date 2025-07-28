package org.llj.shortlink.project.common.interceptor;


import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.llj.shortlink.project.common.Exception.ClientException;
import org.llj.shortlink.project.common.context.BaseContext;
import org.llj.shortlink.project.utils.JwtUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserInterceptor implements HandlerInterceptor {
    private  final String SECRET_KEY = "shortlink-token-secretKey-shortlink-token-secretKey-shortlink-token-secretKey-shortlink-token-secretKey-shortlink-token-secretKey-shortlink-token-secretKey";
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }
        //1、从请求头中获取令牌
        String token = request.getHeader("token");

        //2、校验令牌
        try {
            log.info("jwt校验:{}", token);
            Claims claims = JwtUtil.parseJWT(SECRET_KEY, token);
            if(claims == null || claims.isEmpty()) throw new ClientException("请重新登录");
            String userName = claims.get("username", String.class);

            //将当前用户id放入ThreadLocal中
            BaseContext.setUserName(userName);
            //3、通过，放行
            return true;
        } catch (Exception ex) {
            //4、不通过，响应401状态码
            response.setStatus(401);
            return false;
        }
    }



}
