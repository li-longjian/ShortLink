package org.llj.shortlink.admin.config;

import groovy.util.logging.Slf4j;
import lombok.RequiredArgsConstructor;
import org.llj.shortlink.admin.interceptor.UserFlowRiskControllerInterceptor;
import org.llj.shortlink.admin.interceptor.UserInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class WebMvcConfiguration extends WebMvcConfigurationSupport {
    private final UserInterceptor userInterceptor;
    private final UserFlowRiskControllerInterceptor userFlowRiskControllerInterceptor;
    @Value("${shortlink.flow-limit.enable}")
    private boolean UserFlowControllerEnable;
    /**
     * 注册自定义拦截器
     *
     * @param registry
     */
    protected void addInterceptors(InterceptorRegistry registry) {

        //注册管理端登录拦截器
        registry.addInterceptor(userInterceptor)
                .addPathPatterns("/api/shortlink/**")
                .order(1)
                .excludePathPatterns("/api/shortlink/admin/v1/user/login")
                .excludePathPatterns("/api/shortlink/admin/v1/user/register");

        if(UserFlowControllerEnable){
            registry.addInterceptor(userFlowRiskControllerInterceptor)
                    .order(10)
                    .addPathPatterns("/api/shortlink/**");
        }

    }


    /**
     * 设置静态资源映射
     * @param registry
     */
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
