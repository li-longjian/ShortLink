package org.llj.shortlink.project.common.interceptor;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.llj.shortlink.project.common.Exception.ClientException;
import org.llj.shortlink.project.common.context.BaseContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserInterceptor implements HandlerInterceptor {
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            //1、从请求头中获取用户名
            String userName = request.getHeader("X-User-Name");
            if(userName == null || userName.isEmpty()) throw new ClientException("请重新登录");
            //将当前用户放入ThreadLocal中
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
