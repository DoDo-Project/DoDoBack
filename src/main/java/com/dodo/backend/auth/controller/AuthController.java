package com.dodo.backend.auth.controller;

import com.dodo.backend.auth.dto.request.AuthRequest.LogoutRequest;
import com.dodo.backend.auth.dto.request.AuthRequest.ReissueRequest;
import com.dodo.backend.auth.dto.request.AuthRequest.SocialLoginRequest;
import com.dodo.backend.auth.dto.response.AuthResponse.SocialLoginResponse;
import com.dodo.backend.auth.dto.response.AuthResponse.SocialRegisterResponse;
import com.dodo.backend.auth.dto.response.AuthResponse.TokenResponse;
import com.dodo.backend.auth.service.AuthService;
import com.dodo.backend.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
                    content = @Content(schema = @Schema(implementation = SocialRegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호가 일치하지 않습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"아이디 또는 비밀번호가 일치하지 않습니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "정지된 계정입니다. 또는 휴면 계정입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"정지된 계정 또는 휴면 계정입니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "요청하신 아이디를 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"요청하신 아이디를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "429", description = "요청 횟수 제한을 초과했습니다. 잠시 후 다시 시도해주세요.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "429 Too Many Requests", value = "{\"status\": 429, \"message\": \"요청 횟수 제한을 초과했습니다. 잠시 후 다시 시도해주세요.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping("/social-login")
    public ResponseEntity<?> doSocialLogin(@RequestBody @Valid SocialLoginRequest request, HttpServletRequest httpRequest) {

        String clientIp = httpRequest.getRemoteAddr();

        log.info("소셜 로그인 요청 수신 - provider: {}", request.getProvider());

        authService.checkRateLimit(clientIp);

        return authService.socialLogin(request);
    }

    /**
     * 사용자의 리프레시 토큰을 만료시키고, 현재 사용 중인 액세스 토큰을 블랙리스트에 등록하여 로그아웃을 처리합니다.
     * <p>
     * 1. 전달받은 <b>리프레시 토큰</b>을 Redis에서 삭제하여 토큰 재발급을 차단합니다.<br>
     * 2. 헤더에서 추출한 <b>액세스 토큰</b>을 Redis 블랙리스트에 등록하여, 남은 유효 기간 동안의 접근을 즉시 무효화합니다.
     *
     * @param request     로그아웃할 리프레시 토큰이 담긴 요청 DTO
     * @param httpRequest Authorization 헤더에서 액세스 토큰을 추출하기 위한 요청 객체
     * @return 로그아웃 성공 여부 메시지
     */
    @Operation(summary = "소셜 로그아웃", description = "Refresh Token 삭제 및 Access Token 블랙리스트 처리를 통해 안전하게 로그아웃합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 되었습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "object"),
                            examples = @ExampleObject(name = "200 OK", value = "{\"message\": \"로그아웃 되었습니다.\"}"))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"인증 정보가 유효하지 않습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "로그인 정보를 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"로그인 정보를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody @Valid LogoutRequest request,
                                         HttpServletRequest httpRequest) {

        String bearerToken = httpRequest.getHeader("Authorization");
        String accessToken = null;
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            accessToken = bearerToken.substring(7);
        }

        authService.logout(request, accessToken);

        return ResponseEntity.ok("로그아웃 되었습니다.");
    }

    /**
     * 만료된 Access Token을 갱신하기 위해 새로운 토큰을 요청합니다.
     * <p>
     * Refresh Token Rotation(RTR) 정책을 사용하여, 요청 시 기존 리프레시 토큰은 폐기되고
     * 새로운 리프레시 토큰이 함께 발급됩니다.
     *
     * @param request 기존의 유효한 Refresh Token이 담긴 DTO
     * @return 새로 발급된 Access Token 및 Refresh Token 정보
     */
    @Operation(summary = "토큰 재발급", description = "Refresh Token을 이용하여 Access Token과 Refresh Token을 갱신합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 토큰이 재발급되었습니다.",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "refresh토큰이 만료되었거나 유효하지 않은 토큰입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"refresh토큰이 만료되었습니다.\"}"))), // 메시지 수정 반영
            @ApiResponse(responseCode = "404", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "토큰이 존재하지 않습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "409 Conflict", value = "{\"status\": 409, \"message\": \"토큰이 존재하지 않습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(@RequestBody @Valid ReissueRequest request) {
        log.info("토큰 재발급 요청 수신");
        return ResponseEntity.ok(authService.reissueToken(request));
    }
}