package com.nl2sql.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 网关全局过滤器：
 * <ul>
 *   <li>注入 traceId（已有则透传），下发到下游服务请求头</li>
 *   <li>记录访问日志（方法、路径、耗时、状态）</li>
 *   <li>预留鉴权入口：可在此校验 token 后再 chain.filter，未通过直接 complete</li>
 * </ul>
 */
@Component
public class TraceGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TraceGlobalFilter.class);

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        final String tid = traceId;

        // 鉴权入口预留：
        // if (!authenticated(request)) {
        //     exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        //     return exchange.getResponse().setComplete();
        // }

        ServerHttpRequest mutated = request.mutate()
                .header(TRACE_ID_HEADER, tid)
                .build();

        long start = System.currentTimeMillis();
        String method = request.getMethod().name();
        String path = request.getURI().getPath();

        return chain.filter(exchange.mutate().request(mutated).build())
                .doFinally(signal -> {
                    long cost = System.currentTimeMillis() - start;
                    Integer status = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value() : null;
                    log.info("[{}] {} {} -> {} ({}ms)", tid, method, path, status, cost);
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
