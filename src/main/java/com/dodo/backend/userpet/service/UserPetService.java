package com.dodo.backend.userpet.service;

import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.userpet.entity.RegistrationStatus;

import java.util.Map;
import java.util.UUID;

/**
 * 가족 및 멤버십(UserPet) 도메인의 비즈니스 로직을 정의하는 서비스 인터페이스입니다.
 */
public interface UserPetService {

    /**
     * 사용자와 펫 사이의 관계(멤버십)를 생성하고 저장합니다.
     *
     * @param userId   관계를 맺을 사용자 엔티티
     * @param pet    관계를 맺을 펫 엔티티
     * @param status 등록 상태 (예: APPROVED, PENDING)
     */
    void registerUserPet(UUID userId, Pet pet, RegistrationStatus status);


    /**
     * 가족 초대를 위한 코드를 생성합니다.
     * <p>
     * 요청한 사용자가 해당 반려동물의 승인된(APPROVED) 가족 구성원인지 검증한 후,
     * 초대 코드를 생성하여 Redis에 저장하고 반환합니다.
     *
     * @param userId 요청한 사용자 ID
     * @param petId  반려동물 ID
     * @return 코드("code")와 만료시간("expiredAt")이 담긴 Map 객체
     */
    Map<String, Object> generateInvitationCode(UUID userId, Long petId);
}