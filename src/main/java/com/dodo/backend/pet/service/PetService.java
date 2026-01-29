package com.dodo.backend.pet.service;

import com.dodo.backend.pet.dto.request.PetRequest.PetFamilyJoinRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetRegisterRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetUpdateRequest;
import com.dodo.backend.pet.dto.response.PetResponse;
import com.dodo.backend.pet.dto.response.PetResponse.*;
import com.dodo.backend.pet.entity.Pet;
import org.springframework.data.domain.Pageable;

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

    /**
     * 반려동물 가족 초대를 위한 코드를 발급합니다.
     * <p>
     * 반려동물의 존재 여부를 검증한 후, 실제 코드 생성 및 저장 로직은
     * {@link com.dodo.backend.userpet.service.UserPetService}에 위임합니다.
     *
     * @param userId 요청한 사용자 ID
     * @param petId  반려동물 ID
     * @return 발급된 초대 코드와 만료 시간 정보
     */
    PetInvitationResponse issueInvitationCode(UUID userId, Long petId);

    /**
     * 사용자가 입력한 초대 코드를 통해 반려동물 가족 그룹에 참여합니다.
     * <p>
     * 실제 등록 로직은 {@link com.dodo.backend.userpet.service.UserPetService}에서 수행하며,
     * 결과 데이터를 응답 DTO로 변환하여 반환합니다.
     *
     * @param userId  요청한 사용자의 ID
     * @param request 초대 코드가 포함된 요청 DTO (PetFamilyJoinRequest)
     * @return 펫 정보와 가족 구성원 목록이 담긴 응답 DTO
     */
    PetFamilyJoinResponse joinFamily(UUID userId, PetFamilyJoinRequest request);

    /**
     * 반려동물 ID를 기반으로 Pet 엔티티를 조회합니다.
     * <p>
     * 다른 도메인 서비스(UserPet 등)에서 Pet 엔티티가 필요할 때
     * 리포지토리를 직접 호출하지 않고 이 메소드를 사용합니다.
     *
     * @param petId 조회할 반려동물의 ID
     * @return 조회된 Pet 엔티티
     * @throws com.dodo.backend.pet.exception.PetException 해당 ID의 반려동물이 존재하지 않을 경우
     */
    Pet getPet(Long petId);

    /**
     * 사용자의 반려동물 목록을 페이징하여 조회합니다.
     *
     * @param userId   조회할 사용자의 ID
     * @param pageable 페이징 요청 정보 (page, size)
     * @return 페이징 처리된 반려동물 목록 응답 DTO
     */
    PetListResponse getPetList(UUID userId, Pageable pageable);
}