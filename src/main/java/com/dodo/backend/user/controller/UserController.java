package com.dodo.backend.user.controller;

import com.dodo.backend.auth.dto.response.AuthResponse;
import com.dodo.backend.user.dto.request.UserRequest;
import com.dodo.backend.user.dto.request.UserRequest.UserRegisterRequest;
import com.dodo.backend.user.dto.response.UserResponse;
import com.dodo.backend.user.dto.response.UserResponse.UserRegisterResponse;
import com.dodo.backend.user.service.UserService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Users API", description = "유저 관련 API")
@RequestMapping("/users")
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 회원가입 프로세스의 마지막 단계로, 추가 정보를 입력받아 계정을 활성화합니다.
     * <p>
     * 현재 'REGISTER' 상태인 유저가 닉네임, 지역, 가족여부 필수 정보를 입력하면
     * 상태를 'ACTIVE'로 변경하고, 정회원 권한(USER)을 가진 새로운 토큰을 발급합니다.
     *
     * @param request     닉네임, 전화번호 등 추가 입력 정보가 담긴 DTO
     * @param userDetails SecurityContext에서 추출한 인증 객체 (임시 토큰의 이메일 포함)
     * @return 새로운 Access Token 및 Refresh Token이 포함된 응답 객체
     */
    @Operation(summary = "추가 정보 입력 및 회원가입 완료",
            description = "현재 'REGISTER' 상태인 유저의 추가 정보를 받아 회원가입을 완료합니다. " +
                    "성공 시 계정 상태가 'ACTIVE'로 변경되며, 'USER' 권한을 가진 새로운 Access/Refresh 토큰이 발급됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입이 완료되었습니다.",
                    content = @Content(schema = @Schema(implementation = UserRegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "필수 값이 누락되었거나 형식이 올바르지 않습니다."),
            @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 토큰입니다."),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 닉네임입니다."),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.")
    })
    @PutMapping("/me/profile")
    public ResponseEntity<UserRegisterResponse> completeRegistration(@Valid @RequestBody UserRegisterRequest request,
                                                                     @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        log.info("회원가입 추가 정보 입력 요청 - 이메일: {}", email);
        return ResponseEntity.ok(userService.registerAdditionalInfo(request, email));
    }

    /**
     * 현재 로그인한 사용자의 상세 정보를 조회합니다.
     *
     * @param userDetails SecurityContext에서 추출한 인증 객체
     * @return 유저의 상세 정보 (이메일, 닉네임, 지역 등)
     */
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "유저 정보 조회 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = UserResponse.UserInfoResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다."),
            @ApiResponse(responseCode = "403", description = "사용자를 찾을 수 없습니다."),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다."),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.")
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse.UserInfoResponse> getMyInfo(@AuthenticationPrincipal
                                                                       UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("유저 정보 조회 요청 - Id{}: {}", userId);
        return ResponseEntity.ok(userService.getUserInfo(userId));
    }

}
