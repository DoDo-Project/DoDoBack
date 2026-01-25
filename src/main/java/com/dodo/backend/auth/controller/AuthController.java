package com.dodo.backend.auth.controller;

import com.dodo.backend.auth.dto.request.AuthRequest.SocialLoginRequest;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 및 인가와 관련된 HTTP 요청을 처리하는 컨트롤러 클래스입니다.
 * <p>
 * 소셜 로그인 및 토큰 관리 기능을 제공합니다.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth API", description = "인증/인가 관련 API")
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * 외부 소셜 제공자(Google, Naver)의 인가 코드를 이용해 로그인을 처리합니다.
     * <p>
     * 기존 회원은 로그인을 진행하고, 신규 회원은 회원가입을 위한 임시 토큰을 반환합니다.
     */
    @Operation(summary = "소셜 로그인 (네이버, 구글)", description = "네이버 인가 코드를 받아 로그인 또는 회원가입 분기 처리를 합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공 (토큰 발급)",
                    content = @Content(schema = @Schema(implementation = SocialLoginResponse.class))),
            @ApiResponse(responseCode = "202", description = "회원가입 필요 (임시 토큰 발급)",
                    content = @Content(schema = @Schema(implementation = SocialRegisterResponse.class)))
    })
    @PostMapping("/social-login")
    public ResponseEntity<?> doSocialLogin(@RequestBody @Valid SocialLoginRequest request) {
        log.info("소셜 로그인 요청 수신 - provider: {}", request.getProvider());
        return authService.socialLogin(request);
    }
}
