package com.dodo.backend.pet.service;

import com.dodo.backend.pet.dto.request.PetRequest;
import com.dodo.backend.pet.dto.response.PetResponse;
import com.dodo.backend.pet.dto.response.PetResponse.PetRegisterResponse;

import java.util.UUID;

/**
 * 펫 등록, 조회, 수정 등 반려동물과 관련된 핵심 비즈니스 로직을 정의하는 인터페이스입니다.
 */
public interface PetService {

    /**
     * 사용자의 요청 정보를 바탕으로 새로운 펫을 생성하고, 해당 사용자와의 소유 관계를 설정합니다.
     *
     * @param userId  펫을 등록하려는 사용자의 고유 식별자 (UUID)
     * @param request 펫 등록에 필요한 상세 정보를 담은 요청 객체
     * @return 등록 완료된 펫의 식별자를 포함한 응답 객체
     */
    PetRegisterResponse registerPet(UUID userId, PetRequest.PetRegisterRequest request);
}