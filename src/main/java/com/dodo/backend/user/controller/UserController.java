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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @ApiResponse(responseCode = "200", description = "회원가입 완료 및 토큰 발급 성공")
    @PutMapping("/me/profile")
    public ResponseEntity<UserRegisterResponse> completeRegistration(@Valid @RequestBody UserRegisterRequest request,
                                                                     @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        log.info("회원가입 추가 정보 입력 요청 - 이메일: {}", email);
        return ResponseEntity.ok(userService.registerAdditionalInfo(request, email));
    }

}
