package com.smartcs.user.controller;

import com.smartcs.user.entity.User;
import com.smartcs.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname(),
                "email", user.getEmail(),
                "role", user.getRole()
        );
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id,
                                       @RequestBody Map<String, Object> fields,
                                       Authentication auth) {
        User current = userService.getUserByUsername(auth.getName());
        // 只能修改自己的信息，或者管理员可以修改任意用户
        if (!current.getId().equals(id) && !"ADMIN".equals(current.getRole())) {
            throw new RuntimeException("无权限修改此用户信息");
        }
        userService.updateUser(id, fields);
        return Map.of("success", true, "message", "更新成功");
    }
}