package com.dodo.backend.pet.service;

import com.dodo.backend.pet.dto.request.PetRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetRegisterRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetUpdateRequest;
import com.dodo.backend.pet.dto.response.PetResponse.PetRegisterResponse;
import com.dodo.backend.pet.dto.response.PetResponse.PetUpdateResponse;

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
    PetRegisterResponse registerPet(UUID userId, PetRegisterRequest request);

    /**
     * 반려동물의 정보를 선택적으로 수정합니다.
     * <p>
     * MyBatis의 동적 쿼리를 호출하여 값이 존재하는 필드만 업데이트합니다.
     *
     * @param petId  수정할 반려동물의 고유 ID
     * @param request 수정할 정보를 담은 요청 객체
     * @return 수정이 완료된 후의 최신 반려동물 상세 정보 응답 객체
     */
    PetUpdateResponse updatePet(Long petId, PetUpdateRequest request);
}