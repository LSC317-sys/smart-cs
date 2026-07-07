package com.smartcs.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关服务启动类
 * 
 * @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan
 *   - @Configuration: 声明这是一个配置类，Spring 会扫描其中的 @Bean 方法
 *   - @EnableAutoConfiguration: 自动配置 Redis、Web、Security 等（不用自己配大部分配置）
 *   - @ComponentScan: 自动扫描当前包及子包下的 @Component/@Service/@Repository/@Controller
 * 
 * @EnableDiscoveryClient: 启用服务注册发现（Nacos/Consul/Eureka）
 *   - 启动时自动向注册中心注册服务，供其他服务发现
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {
    public static void main(String[] args) {
        // 启动 Spring Boot 应用
        // 参数：启动类.class，系统命令行传入的 args
        SpringApplication.run(GatewayApplication.class, args);
    }
}