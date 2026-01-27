package com.dodo.backend.userpet.service;

import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.userpet.entity.RegistrationStatus;

/**
 * 가족 및 멤버십(UserPet) 도메인의 비즈니스 로직을 정의하는 서비스 인터페이스입니다.
 */
public interface UserPetService {

    /**
     * 사용자와 펫 사이의 관계(멤버십)를 생성하고 저장합니다.
     *
     * @param user   관계를 맺을 사용자 엔티티
     * @param pet    관계를 맺을 펫 엔티티
     * @param status 등록 상태 (예: APPROVED, PENDING)
     */
    void registerUserPet(User user, Pet pet, RegistrationStatus status);
}