package com.smartcs.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.sql.Connection;
import org.springframework.data.redis.connection.RedisConnection;

@RestController
@RequestMapping("/admin")
public class HealthController {

    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("service", "smart-cs-admin");
        
        // 检查 MySQL
        Map<String, Object> mysql = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            mysql.put("status", "UP");
            mysql.put("database", conn.getCatalog());
        } catch (SQLException e) {
            mysql.put("status", "DOWN");
            mysql.put("error", e.getMessage());
        }
        health.put("mysql", mysql);
        
        // 检查 Redis
        Map<String, Object> redis = new HashMap<>();
        try {
            RedisConnection connection = redisConnectionFactory.getConnection();
            connection.ping();
            connection.close();
            redis.put("status", "UP");
        } catch (Exception e) {
            redis.put("status", "DOWN");
            redis.put("error", e.getMessage());
        }
        health.put("redis", redis);
        
        return ResponseEntity.ok(health);
    }
    
    @GetMapping("/health/live")
    public ResponseEntity<Map<String, String>> liveness() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
    
    @GetMapping("/health/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        // 就绪探针：检查所有依赖
        Map<String, Object> status = new HashMap<>();
        boolean allUp = true;
        
        try (Connection conn = dataSource.getConnection()) {
            status.put("mysql", "UP");
        } catch (SQLException e) {
            status.put("mysql", "DOWN");
            allUp = false;
        }
        
        try {
            RedisConnection connection = redisConnectionFactory.getConnection();
            connection.ping();
            connection.close();
            status.put("redis", "UP");
        } catch (Exception e) {
            status.put("redis", "DOWN");
            allUp = false;
        }
        
        status.put("status", allUp ? "UP" : "DOWN");
        return ResponseEntity.status(allUp ? 200 : 503).body(status);
    }
}