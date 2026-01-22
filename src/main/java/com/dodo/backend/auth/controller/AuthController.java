package com.dodo.backend.auth.controller;

import com.dodo.backend.auth.dto.request.AuthRequest;
import com.dodo.backend.auth.dto.request.AuthRequest.SocialLoginRequest;
import com.dodo.backend.auth.dto.response.AuthResponse;
import com.dodo.backend.auth.dto.response.AuthResponse.SocialLoginResponse;
import com.dodo.backend.auth.dto.response.AuthResponse.SocialRegisterResponse;
import com.dodo.backend.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth API", description = "인증/인가 관련 API")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "소셜 로그인 (네이버, 구글)", description = "네이버 인가 코드를 받아 로그인 또는 회원가입 분기 처리를 합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공 (토큰 발급)",
                    content = @Content(schema = @Schema(implementation = SocialLoginResponse.class))),
            @ApiResponse(responseCode = "202", description = "회원가입 필요 (임시 토큰 발급)",
                    content = @Content(schema = @Schema(implementation = SocialRegisterResponse.class)))
    })
    @PostMapping("/social-login")
    public ResponseEntity<?> doSocialLogin(@RequestBody @Valid SocialLoginRequest request) {
        return authService.socialLogin(request);
    }
}
