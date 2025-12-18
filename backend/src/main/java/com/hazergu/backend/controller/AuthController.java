package com.hazergu.backend.controller;

import com.hazergu.backend.entity.AuthRequest;
import com.hazergu.backend.entity.User;
import com.hazergu.backend.repository.UserRepository;
import com.hazergu.backend.util.JwtUtil;
import com.hazergu.backend.util.PasswordEncoderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private PasswordEncoderUtil passwordEncoderUtil;
    
    /**
     * 注册接口
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest authRequest) {
        // 检查用户名是否已存在
        Optional<User> existingUser = userRepository.findByUsername(authRequest.getUsername());
        if (existingUser.isPresent()) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "用户名已存在");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        
        // 创建新用户，加密密码
        User user = new User();
        user.setUsername(authRequest.getUsername());
        user.setPassword(passwordEncoderUtil.encodePassword(authRequest.getPassword()));
        user.setIsAdmin(0); // 默认为普通用户
        
        userRepository.save(user);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "注册成功");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 登录接口
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        // 查找用户
        Optional<User> optionalUser = userRepository.findByUsername(authRequest.getUsername());
        if (!optionalUser.isPresent()) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "用户名或密码错误");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        
        User user = optionalUser.get();
        
        // 验证密码
        if (!passwordEncoderUtil.matchesPassword(authRequest.getPassword(), user.getPassword())) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "用户名或密码错误");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        
        // 生成 JWT 令牌
        String token = jwtUtil.generateToken(user.getUsername());
        
        // 返回令牌和用户信息
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("username", user.getUsername());
        response.put("isAdmin", user.getIsAdmin());
        
        return ResponseEntity.ok(response);
    }
}