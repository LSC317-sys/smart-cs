package com.smartcs.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 路由配置 - 使用 Java DSL 方式定义路由
 * 服务地址支持环境变量覆盖，用于 Docker 部署
 */
@Configuration
public class RouteConfig {

    // ========== 服务地址（支持环境变量覆盖） ==========
    @Value("${chat.service.url:http://127.0.0.1:8081}")
    private String chatService;

    @Value("${knowledge.service.url:http://127.0.0.1:8082}")
    private String knowledgeService;

    @Value("${user.service.url:http://127.0.0.1:8083}")
    private String userService;

    @Value("${admin.service.url:http://127.0.0.1:8084}")
    private String adminService;

    // ========== CORS 配置常量 ==========
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:3000",
            "http://localhost:5173",
            "http://localhost:8080",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:5173",
            "http://121.199.172.24",
            "http://121.199.172.24:80",
            "http://121.199.172.24:8080"
    );
    private static final String CORS_METHODS = "GET, POST, PUT, DELETE, OPTIONS, PATCH";
    private static final String CORS_HEADERS = "Authorization, Content-Type, X-Requested-With, Accept, Origin, Cache-Control";
    private static final String CORS_EXPOSE_HEADERS = "Content-Length, Content-Type";

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // ========== Chat 服务 ==========
                .route("chat-ask", r -> r
                        .path("/api/chat/ask")
                        .filters(f -> f.stripPrefix(1))
                        .uri(chatService))
                .route("chat-chat", r -> r
                        .path("/api/chat/chat")
                        .filters(f -> f.stripPrefix(1))
                        .uri(chatService))
                .route("chat-chat-stream", r -> r
                        .path("/api/chat/chat/stream")
                        .filters(f -> f.stripPrefix(1))
                        .uri(chatService))
                .route("chat-stream", r -> r
                        .path("/api/chat/stream")
                        .filters(f -> f.stripPrefix(1))
                        .uri(chatService))
                .route("chat-sse", r -> r
                        .path("/api/chat/sse")
                        .filters(f -> f.stripPrefix(1))
                        .uri(chatService))
                .route("chat-session", r -> r
                        .path("/api/chat/session/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(chatService))
                .route("chat-sessions", r -> r
                        .path("/api/chat/sessions")
                        .filters(f -> f.stripPrefix(1))
                        .uri(chatService))
                .route("chat-config", r -> r
                        .path("/api/chat/config")
                        .filters(f -> f.stripPrefix(1))
                        .uri(chatService))
                .route("chat-feedback", r -> r
                        .path("/api/chat/feedback/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(chatService))
                .route("chat-direct", r -> r
                        .path("/chat/**")
                        .uri(chatService))

                // ========== Knowledge 服务 ==========
                .route("knowledge-upload", r -> r
                        .path("/api/knowledge/documents/upload")
                        .filters(f -> f.stripPrefix(1))
                        .uri(knowledgeService))
                .route("knowledge-list", r -> r
                        .path("/api/knowledge/documents")
                        .filters(f -> f.stripPrefix(1))
                        .uri(knowledgeService))
                .route("knowledge-delete", r -> r
                        .path("/api/knowledge/del/**")
                        .filters(f -> f.stripPrefix(1).rewritePath("/api/knowledge/del(/.*)", "/knowledge/documents$1"))
                        .uri(knowledgeService))
                .route("knowledge-detail", r -> r
                        .path("/api/knowledge/documents/{id}")
                        .filters(f -> f.stripPrefix(1))
                        .uri(knowledgeService))
                .route("knowledge-content", r -> r
                        .path("/api/knowledge/documents/{id}/content")
                        .filters(f -> f.stripPrefix(1))
                        .uri(knowledgeService))
                .route("knowledge-stats", r -> r
                        .path("/api/knowledge/stats")
                        .filters(f -> f.stripPrefix(1))
                        .uri(knowledgeService))
                .route("knowledge-kb", r -> r
                        .path("/api/knowledge/kb/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(knowledgeService))
                .route("knowledge-versions", r -> r
                        .path("/api/knowledge/documents/{id}/versions")
                        .filters(f -> f.stripPrefix(1))
                        .uri(knowledgeService))
                .route("knowledge-version-detail", r -> r
                        .path("/api/knowledge/documents/{id}/versions/{version}")
                        .filters(f -> f.stripPrefix(1))
                        .uri(knowledgeService))
                .route("knowledge-update", r -> r
                        .path("/api/knowledge/documents/{id}/update")
                        .filters(f -> f.stripPrefix(1))
                        .uri(knowledgeService))
                .route("knowledge-rollback", r -> r
                        .path("/api/knowledge/documents/{id}/rollback")
                        .filters(f -> f.stripPrefix(1))
                        .uri(knowledgeService))
                .route("knowledge-direct", r -> r
                        .path("/knowledge/**")
                        .uri(knowledgeService))

                // ========== User 服务 ==========
                .route("auth-register", r -> r
                        .path("/api/auth/register")
                        .filters(f -> f.stripPrefix(1))
                        .uri(userService))
                .route("auth-login", r -> r
                        .path("/api/auth/login")
                        .filters(f -> f.stripPrefix(1))
                        .uri(userService))
                .route("auth-catchall", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(userService))
                .route("user-profile", r -> r
                        .path("/api/users/profile")
                        .filters(f -> f.stripPrefix(1))
                        .uri(userService))
                .route("user-list", r -> r
                        .path("/api/users")
                        .filters(f -> f.stripPrefix(1))
                        .uri(userService))
                .route("user-catchall", r -> r
                        .path("/api/users/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(userService))
                .route("users-direct", r -> r
                        .path("/users/**")
                        .uri(userService))
                .route("auth-direct", r -> r
                        .path("/auth/**")
                        .uri(userService))

                // ========== Admin 服务 ==========
                .route("admin-overview", r -> r
                        .path("/api/admin/stats/overview")
                        .filters(f -> f.stripPrefix(1))
                        .uri(adminService))
                .route("admin-conversations", r -> r
                        .path("/api/admin/stats/conversations")
                        .filters(f -> f.stripPrefix(1))
                        .uri(adminService))
                .route("admin-clear", r -> r
                        .path("/api/admin/stats/conversations/*/clear")
                        .filters(f -> f.stripPrefix(1))
                        .uri(adminService))
                .route("admin-knowledge", r -> r
                        .path("/api/admin/stats/knowledge")
                        .filters(f -> f.stripPrefix(1))
                        .uri(adminService))
                .route("admin-config", r -> r
                        .path("/api/admin/config/models")
                        .filters(f -> f.stripPrefix(1))
                        .uri(adminService))
                .route("admin-feedback-stats", r -> r
                        .path("/api/admin/stats/feedback")
                        .filters(f -> f.stripPrefix(1))
                        .uri(adminService))
                .route("admin-feedback-list", r -> r
                        .path("/api/admin/stats/feedback/list")
                        .filters(f -> f.stripPrefix(1))
                        .uri(adminService))
                .route("admin-direct", r -> r
                        .path("/admin/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri(adminService))

                .build();
    }

    /**
     * CORS 全局过滤器
     * 对所有请求（含 OPTIONS 预检）添加 CORS 头
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public WebFilter corsFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String origin = request.getHeaders().getOrigin();
            boolean isAllowed = ALLOWED_ORIGINS.contains(origin);

            ServerHttpResponse response = exchange.getResponse();
            if (isAllowed) {
                response.getHeaders().add("Access-Control-Allow-Origin", origin);
                response.getHeaders().add("Access-Control-Allow-Methods", CORS_METHODS);
                response.getHeaders().add("Access-Control-Allow-Headers", CORS_HEADERS);
                response.getHeaders().add("Access-Control-Allow-Credentials", "true");
                response.getHeaders().add("Access-Control-Max-Age", "3600");
                response.getHeaders().add("Access-Control-Expose-Headers", CORS_EXPOSE_HEADERS);
            }

            if (request.getMethod() == HttpMethod.OPTIONS) {
                response.setStatusCode(HttpStatus.OK);
                return Mono.empty();
            }

            return chain.filter(exchange);
        };
    }

    /**
     * Gateway 健康检查端点
     */
    @Bean
    public WebFilter healthFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            if (path.equals("/gateway/health") || path.equals("/health")) {
                ServerHttpResponse response = exchange.getResponse();
                response.getHeaders().add("Content-Type", "application/json; charset=utf-8");
                response.setStatusCode(HttpStatus.OK);
                String body = "{\"status\":\"UP\",\"service\":\"smart-cs-gateway\",\"port\":8080}";
                DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
                return response.writeWith(Flux.just(buffer));
            }
            return chain.filter(exchange);
        };
    }
}
