package com.dodo.backend.user.service;

import com.dodo.backend.user.dto.request.UserRequest.UserRegisterRequest;
import com.dodo.backend.user.dto.response.UserResponse.UserInfoResponse;
import com.dodo.backend.user.dto.response.UserResponse.UserRegisterResponse;

import java.util.Map;
import java.util.UUID;

/**
 * 사용자 정보 관리 및 회원 관련 비즈니스 로직을 담당하는 서비스 인터페이스입니다.
 * <p>
 * 신규 사용자의 추가 정보 등록, 소셜 계정 연동을 통한 유저 식별 및 저장,
 * 그리고 계정 탈퇴를 위한 인증 프로세스 등 사용자 생명주기 전반에 걸친 기능을 정의합니다.
 */
public interface UserService {

    /**
     * 회원가입 시 수집되는 사용자의 추가 정보를 시스템에 등록합니다.
     * <p>
     * 소셜 로그인 이후 서비스 이용에 필요한 최소한의 프로필 정보를 저장하고,
     * 정식 회원으로의 전환 처리를 수행합니다.
     *
     * @param request 등록할 사용자 추가 정보 DTO
     * @param email 정보를 등록할 대상 사용자의 이메일 주소
     * @return 등록 완료 후의 사용자 정보 및 처리 결과 응답 DTO
     */
    UserRegisterResponse registerAdditionalInfo(UserRegisterRequest request, String email);

    /**
     * 외부 소셜 제공자로부터 전달받은 정보를 기반으로 유저를 조회하거나 신규 등록합니다.
     * <p>
     * 시스템에 이미 존재하는 이메일인 경우 해당 유저 정보를 반환하며,
     * 존재하지 않는 경우 신규 유저로 생성하여 데이터베이스에 영속화합니다.
     *
     * @param email 소셜 계정의 이메일 주소
     * @param name 사용자의 이름 또는 별명
     * @param profileImage 소셜 프로필 이미지 URL
     * @return 조회 또는 저장된 유저 정보가 담긴 Map 객체
     */
    Map<String, Object> findOrSaveSocialUser(String email, String name, String profileImage);

    /**
     * 고유 식별자(UUID)를 기반으로 사용자의 상세 정보를 조회합니다.
     *
     * @param userId 조회를 수행할 사용자의 고유 ID
     * @return 해당 사용자의 프로필 및 계정 상태 정보를 포함한 응답 DTO
     */
    UserInfoResponse getUserInfo(UUID userId);

    /**
     * 계정 탈퇴를 위한 본인 확인 인증 메일 발송을 요청합니다.
     * <p>
     * 내부적으로 메일 서비스를 호출하여 보안 인증 코드를 발송하며,
     * 탈퇴 절차의 첫 번째 단계인 본인 확인 프로세스를 시작합니다.
     *
     * @param userId 탈퇴 인증을 진행할 사용자의 아이디
     */
    void requestWithdrawal(UUID userId);

    /**
     * 사용자가 입력한 인증 번호를 검증하고 최종 탈퇴 처리를 수행합니다.
     *
     * @param userId   탈퇴를 진행할 유저의 UUID
     * @param authCode 사용자가 입력한 6자리 인증 번호
     */
    void deleteWithdrawal(UUID userId, String authCode);
}