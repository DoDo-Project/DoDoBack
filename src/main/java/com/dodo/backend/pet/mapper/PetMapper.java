package com.dodo.backend.pet.mapper;

import com.dodo.backend.pet.dto.request.PetRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetUpdateRequest;
import com.dodo.backend.pet.entity.Pet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PetMapper {

    /**
     * 반려동물의 프로필 정보를 선택적으로 업데이트합니다.
     * DTO를 직접 파라미터로 받아 String 타입 데이터를 그대로 쿼리에 사용합니다.
     *
     * @param request 수정할 데이터가 담긴 DTO
     * @param petId   수정할 반려동물의 식별자
     */
    void updatePetProfileInfo(@Param("request") PetUpdateRequest request, @Param("petId") Long petId);

    /**
     * 펫의 디바이스 ID를 업데이트합니다.
     *
     * @param petId    업데이트할 펫 ID
     * @param deviceId 새로운 디바이스 ID
     */
    void updatePetDevice(@Param("petId") Long petId, @Param("deviceId") String deviceId);
}
