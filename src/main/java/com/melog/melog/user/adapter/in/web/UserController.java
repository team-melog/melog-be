package com.melog.melog.user.adapter.in.web;

import com.melog.melog.user.domain.model.request.UserCreateRequest;
import com.melog.melog.user.domain.model.request.UserUpdateRequest;
import com.melog.melog.user.domain.model.response.UserResponse;
import com.melog.melog.user.application.port.in.UserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;

    /**
     * 사용자 생성
     * POST /api/users
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserCreateRequest request) {
        UserResponse response = userUseCase.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 사용자 정보 조회
     * GET /api/users/{nickname}
     */
    @GetMapping("/{nickname}")
    public ResponseEntity<UserResponse> getUser(@PathVariable(value = "nickname", required = true) String nickname) {
        // 한글 포함 여부 확인 후 필요시에만 디코딩
        String decodedNickname = nickname.contains("%") ? 
            java.net.URLDecoder.decode(nickname, java.nio.charset.StandardCharsets.UTF_8) : nickname;
        
        UserResponse response = userUseCase.getUserByNickname(decodedNickname);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 닉네임 수정
     * PUT /api/users/{nickname}
     */
    @PutMapping("/{nickname}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable(value = "nickname", required = true) String nickname, 
                                                 @RequestBody UserUpdateRequest request) {
        // 한글 포함 여부 확인 후 필요시에만 디코딩
        String decodedNickname = nickname.contains("%") ? 
            java.net.URLDecoder.decode(nickname, java.nio.charset.StandardCharsets.UTF_8) : nickname;
        
        UserResponse response = userUseCase.updateUserNickname(decodedNickname, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 삭제
     * DELETE /api/users/{nickname}
     */
    @DeleteMapping("/{nickname}")
    public ResponseEntity<Void> deleteUser(@PathVariable(value = "nickname", required = true) String nickname) {
        // 한글 포함 여부 확인 후 필요시에만 디코딩
        String decodedNickname = nickname.contains("%") ? 
            java.net.URLDecoder.decode(nickname, java.nio.charset.StandardCharsets.UTF_8) : nickname;
        
        userUseCase.deleteUser(decodedNickname);
        return ResponseEntity.noContent().build();
    }
} 