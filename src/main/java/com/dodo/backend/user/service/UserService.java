package com.dodo.backend.user.service;

import com.dodo.backend.user.dto.request.UserRequest.UserRegisterRequest;
import com.dodo.backend.user.dto.response.UserResponse.UserRegisterResponse;

import java.util.Map;

public interface UserService {

    /**
     * 회원가입 추가 정보를 등록합니다.
     */
    UserRegisterResponse registerAdditionalInfo(UserRegisterRequest request, String email);

    /**
     * 소셜 로그인 정보를 받아 유저를 조회하거나 신규 저장합니다.
     * (AuthService에서 소셜 API 통신 완료 후 호출)
     */
    Map<String, Object> findOrSaveSocialUser(String email, String name, String profileImage);
}