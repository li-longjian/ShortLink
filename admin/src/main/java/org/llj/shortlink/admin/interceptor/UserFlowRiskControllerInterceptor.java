package org.llj.shortlink.admin.interceptor;

import com.alibaba.fastjson.JSON;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.llj.shortlink.admin.common.Exception.ClientException;
import org.llj.shortlink.admin.common.context.BaseContext;
import org.llj.shortlink.admin.common.convention.result.Results;
import org.llj.shortlink.admin.config.UserFlowRiskControllerConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserFlowRiskControllerInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate stringRedisTemplate;
    private final UserFlowRiskControllerConfiguration userFlowRiskControllerConfiguration;
    private static final String  USER_FLOW_RISK_CONTROL_LUA_SCRIPT_PATH = "lua/user_flow_risk_control.lua";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        /**
         * 设置lua脚本导入加载
         */
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(USER_FLOW_RISK_CONTROL_LUA_SCRIPT_PATH)));
        redisScript.setResultType(Long.class);
        String username = Optional.ofNullable(BaseContext.getUserName()).orElse("other");
        ArrayList<String> strings = new ArrayList<>();
        strings.add(username);
        Long result = null;
        try{
            result = stringRedisTemplate.execute(redisScript, strings ,userFlowRiskControllerConfiguration.getTimeWindow());
        }catch (Exception e){
            log.error("执行用户请求流量限制LUA脚本出错");
            returnJson(response, JSON.toJSONString(Results.failure(new ClientException("当前系统繁忙，请稍后再试"))));
            return false;
        }
        if (result == null || result > userFlowRiskControllerConfiguration.getMaxAccessCount()) {
            returnJson(response, JSON.toJSONString(Results.failure(new ClientException("当前系统繁忙，请稍后再试"))));
        }
        return true;
    }

    private void returnJson(HttpServletResponse response, String json) throws Exception {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=utf-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.print(json);
        }
    }
}
