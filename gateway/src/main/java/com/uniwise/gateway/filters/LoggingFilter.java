package com.uniwise.gateway.filters;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.uniwise.common.ansi.HttpMethodColor;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class LoggingFilter implements GlobalFilter{
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();

        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();

        String traceId = exchange.getRequest().getId();
        log.info("{}{}{} [{}] -> {}",
                HttpMethodColor.color(method), method, HttpMethodColor.RESET,
                traceId,
                path
        );
        return chain.filter(exchange).doOnSuccess(aVoid -> {
                    int status = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value()
                            : 200;

                    long duration = System.currentTimeMillis() - startTime;

                    log.info("{}{}{} [{}] -> {} {} ({} ms)",
                            HttpMethodColor.color(method), method, HttpMethodColor.RESET,
                            traceId,
                            path,
                            HttpMethodColor.statusColor(status) + status + HttpMethodColor.RESET,
                            duration
                    );
                }
        );
    }
}
