package com.melog.melog.user.adapter.in.web;

import com.melog.melog.common.model.request.UserCreateRequest;
import com.melog.melog.common.model.request.UserUpdateRequest;
import com.melog.melog.common.model.response.UserResponse;
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
    public ResponseEntity<UserResponse> getUser(@PathVariable String nickname) {
        UserResponse response = userUseCase.getUserByNickname(nickname);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 닉네임 수정
     * PUT /api/users/{nickname}
     */
    @PutMapping("/{nickname}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable String nickname, 
                                                 @RequestBody UserUpdateRequest request) {
        UserResponse response = userUseCase.updateUserNickname(nickname, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 삭제
     * DELETE /api/users/{nickname}
     */
    @DeleteMapping("/{nickname}")
    public ResponseEntity<Void> deleteUser(@PathVariable String nickname) {
        userUseCase.deleteUser(nickname);
        return ResponseEntity.noContent().build();
    }
} 