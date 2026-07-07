package com.smartcs.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartcs.user.dto.AdminRegisterRequest;
import com.smartcs.user.dto.LoginRequest;
import com.smartcs.user.dto.LoginResponse;
import com.smartcs.user.dto.RegisterRequest;
import com.smartcs.user.entity.User;
import com.smartcs.user.mapper.UserMapper;
import com.smartcs.user.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    @Value("${admin.invitation-code}")
    private String adminInvitationCode;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("账号已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole(),
                Map.of("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername()));

        log.info("用户登录成功: {}", user.getUsername());

        return new LoginResponse(
                token,
                user.getUsername(),
                user.getNickname() != null ? user.getNickname() : user.getUsername(),
                user.getRole(),
                jwtUtil.getExpiration()
        );
    }

    public User register(RegisterRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setRole("USER");
        user.setStatus("ACTIVE");
        userMapper.insert(user);

        log.info("用户注册成功: {}", user.getUsername());
        return user;
    }

    public User registerAdmin(AdminRegisterRequest request, String validInvitationCode) {
        // 验证邀请码
        if (validInvitationCode == null || !validInvitationCode.equals(request.getInvitationCode())) {
            throw new RuntimeException("邀请码错误，无法注册管理员账号");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setRole("ADMIN");
        user.setStatus("ACTIVE");
        userMapper.insert(user);

        log.info("管理员注册成功: {}", user.getUsername());
        return user;
    }

    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        if (user == null) throw new RuntimeException("用户不存在: " + username);
        return user;
    }

    public void updateUser(Long id, Map<String, Object> fields) {
        User user = userMapper.selectById(id);
        if (user == null) throw new RuntimeException("用户不存在");
        if (fields.containsKey("nickname")) user.setNickname((String) fields.get("nickname"));
        if (fields.containsKey("email")) user.setEmail((String) fields.get("email"));
        if (fields.containsKey("password")) user.setPassword(passwordEncoder.encode((String) fields.get("password")));
        if (fields.containsKey("status")) user.setStatus((String) fields.get("status"));
        userMapper.updateById(user);
    }
}