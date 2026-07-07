package com.smartcs.user.controller;

import com.smartcs.user.dto.AdminRegisterRequest;
import com.smartcs.user.dto.LoginRequest;
import com.smartcs.user.dto.LoginResponse;
import com.smartcs.user.dto.RegisterRequest;
import com.smartcs.user.entity.User;
import com.smartcs.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    @Value("${admin.invitation-code}")
    private String adminInvitationCode;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return Map.of(
                "success", true,
                "userId", user.getId(),
                "username", user.getUsername(),
                "message", "注册成功"
        );
    }

    @PostMapping("/register/admin")
    public Map<String, Object> registerAdmin(@Valid @RequestBody AdminRegisterRequest request) {
        User user = userService.registerAdmin(request, adminInvitationCode);
        return Map.of(
                "success", true,
                "userId", user.getId(),
                "username", user.getUsername(),
                "message", "管理员注册成功"
        );
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }
}