package org.llj.shortlink.gateway.filter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.llj.shortlink.gateway.config.Config;
import org.llj.shortlink.gateway.dto.GatewayErrorResult;
import org.llj.shortlink.gateway.utils.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * SpringCloud Gateway Token 校验
 */
@Slf4j
@Component
public class TokenValidateGatewayFilterFactory extends AbstractGatewayFilterFactory<Config> {
    private  final String SECRET_KEY = "shortlink-token-secretKey-shortlink-token-secretKey-shortlink-" +
            "token-secretKey-shortlink-token-secretKey-shortlink-token-secretKey-shortlink-token-secretKey";
    public TokenValidateGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String requestPath = request.getPath().toString();
            String requestMethod = request.getMethod().name();
            List<String> whitePathList = config.getWhitePathList();

            if (!isPathInWhiteList(requestPath, requestMethod, whitePathList)) {

                String token = request.getHeaders().getFirst("token");
                // JWT 校验token
                //校验令牌
                try {
                    log.info("jwt校验:{}", token);
                    Claims claims = JwtUtil.parseJWT(SECRET_KEY, token);
                    if(claims == null || claims.isEmpty()) throw new RuntimeException("登录过期,请重新登录");
                    String userName = claims.get("username", String.class);
                    //将当前用户名放入请求头中,传给后面的微服务模块
                    // 将用户信息添加到请求头中
                    ServerHttpRequest modifiedRequest = request.mutate()
                            .header("X-User-Name", userName)
                            .build();
                    //3、通过，放行
                    //校验通过
                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                } catch (Exception ex) {
                    //4、不通过，响应401状态码
                    //校验不通过
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return response.writeWith(Mono.fromSupplier(() -> {
                        DataBufferFactory bufferFactory = response.bufferFactory();
                        GatewayErrorResult resultMessage = GatewayErrorResult.builder()
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .message("Token validation error")
                                .build();
                        return bufferFactory.wrap(JSON.toJSONString(resultMessage).getBytes());
                    }));
                }
            }
            // 白名单路径直接放行
            return chain.filter(exchange);
        };
    }

    private boolean isPathInWhiteList(String requestPath, String requestMethod, List<String> whitePathList) {
        return (!CollectionUtils.isEmpty(whitePathList) && whitePathList.stream().anyMatch(requestPath::startsWith)) || (Objects.equals(requestPath, "/api/short-link/admin/v1/user") && Objects.equals(requestMethod, "POST"));
    }
}
