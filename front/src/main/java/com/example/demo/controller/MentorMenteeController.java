package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.entity.MatchRequest;
import com.example.demo.service.UserService;
import com.example.demo.service.MatchRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.io.File;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api")
public class MentorMenteeController {
    @Autowired
    private UserService userService;
    @Autowired
    private MatchRequestService matchRequestService;

    // 회원가입
    @PostMapping(value = "/signup", consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, Object>> signup(
            @RequestPart("user") Map<String, Object> payload,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        if (!payload.containsKey("email") || !payload.containsKey("password") || !payload.containsKey("name") || !payload.containsKey("role")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid payload"));
        }
        User user = new User();
        user.setEmail((String) payload.get("email"));
        user.setPassword((String) payload.get("password"));
        user.setName((String) payload.get("name"));
        user.setRole((String) payload.get("role"));
        user.setBio((String) payload.getOrDefault("bio", ""));
        Object skillsObj = payload.getOrDefault("skills", List.of());
        if (skillsObj instanceof List<?>) {
            List<?> rawList = (List<?>) skillsObj;
            List<String> skills = new ArrayList<>();
            for (Object o : rawList) {
                if (o instanceof String s) skills.add(s);
            }
            user.setSkills(skills);
        } else {
            user.setSkills(List.of());
        }
        if (profileImage != null && !profileImage.isEmpty()) {
            String fileName = System.currentTimeMillis() + "_" + StringUtils.cleanPath(profileImage.getOriginalFilename());
            String uploadDir = "uploads/profile/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            File dest = new File(uploadDir + fileName);
            try {
                profileImage.transferTo(dest);
                user.setImageUrl("/" + uploadDir + fileName);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "File upload failed"));
            }
        }
        userService.saveUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "User created"));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> payload) {
        if (!payload.containsKey("email") || !payload.containsKey("password")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid payload"));
        }
        String email = (String) payload.get("email");
        String password = (String) payload.get("password");
        User user = userService.findByEmail(email);
        if (user == null || !user.getPassword().equals(password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid email or password"));
        }
        // JWT 토큰 생성
        String jwtSecret = "mySecretKey123456"; // 실제 서비스에서는 환경변수로 관리
        String token = Jwts.builder()
                .setSubject(user.getEmail())
                .claim("id", user.getId())
                .claim("role", user.getRole())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 1일
                .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes())
                .compact();
        return ResponseEntity.ok(Map.of("token", token));
    }

    // 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@RequestHeader("Authorization") String auth) {
        // JWT 토큰에서 사용자 ID 추출
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing or invalid token"));
        }
        String token = auth.substring(7);
        String jwtSecret = "mySecretKey123456";
        Long userId;
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret.getBytes())
                .parseClaimsJws(token)
                .getBody();
            userId = claims.get("id", Long.class);
        } catch (JwtException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
        User user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }
        Map<String, Object> profile = Map.of(
            "name", user.getName(),
            "bio", user.getBio(),
            "imageUrl", user.getImageUrl(),
            "skills", user.getSkills()
        );
        Map<String, Object> result = Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "role", user.getRole(),
            "profile", profile
        );
        return ResponseEntity.ok(result);
    }

    // 내 정보 조회 (id 기반)
    @GetMapping("/me/{id}")
    public ResponseEntity<Map<String, Object>> meById(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }
        Map<String, Object> profile = Map.of(
            "name", user.getName(),
            "bio", user.getBio(),
            "imageUrl", user.getImageUrl(),
            "skills", user.getSkills()
        );
        Map<String, Object> result = Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "role", user.getRole(),
            "profile", profile
        );
        return ResponseEntity.ok(result);
    }

    // 멘토 리스트 조회 (DB 기반)
    @GetMapping("/mentors")
    public ResponseEntity<List<Map<String, Object>>> mentors(@RequestHeader("Authorization") String auth) {
        // JWT 인증 생략(간단화)
        List<User> mentorUsers = userService.findByRole("mentor");
        List<Map<String, Object>> mentors = new ArrayList<>();
        for (User user : mentorUsers) {
            Map<String, Object> profile = Map.of(
                "name", user.getName(),
                "bio", user.getBio(),
                "imageUrl", user.getImageUrl(),
                "skills", user.getSkills()
            );
            Map<String, Object> mentor = Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "role", user.getRole(), // mentor/mentee 구분
                "isMentor", "mentor".equals(user.getRole()),
                "profile", profile
            );
            mentors.add(mentor);
        }
        return ResponseEntity.ok(mentors);
    }

    // 매칭 요청 보내기 (DB 저장)
    @PostMapping("/match-requests")
    public ResponseEntity<Map<String, Object>> matchRequest(@RequestHeader("Authorization") String auth, @RequestBody Map<String, Object> payload) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing or invalid token"));
        }
        String token = auth.substring(7);
        String jwtSecret = "mySecretKey123456";
        Long menteeId;
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret.getBytes())
                .parseClaimsJws(token)
                .getBody();
            menteeId = claims.get("id", Long.class);
        } catch (JwtException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
        }
        Long mentorId = ((Number) payload.getOrDefault("mentorId", 0)).longValue();
        String message = (String) payload.getOrDefault("message", "멘토링 받고 싶어요!");
        User mentor = userService.findById(mentorId);
        User mentee = userService.findById(menteeId);
        if (mentor == null || mentee == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid mentor or mentee"));
        }
        MatchRequest req = new MatchRequest();
        req.setMentor(mentor);
        req.setMentee(mentee);
        req.setMessage(message);
        req.setStatus("pending");
        MatchRequest saved = matchRequestService.save(req);
        Map<String, Object> response = Map.of(
            "id", saved.getId(),
            "mentorId", mentor.getId(),
            "menteeId", mentee.getId(),
            "message", saved.getMessage(),
            "status", saved.getStatus(),
            "createdAt", saved.getCreatedAt()
        );
        return ResponseEntity.ok(response);
    }
}
