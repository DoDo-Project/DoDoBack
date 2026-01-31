package com.dodo.backend.userpet.mapper;

import com.dodo.backend.userpet.entity.RegistrationStatus;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

@Mapper
public interface UserPetMapper {

    /**
     * 특정 유저와 펫의 관계 상태(RegistrationStatus)를 변경합니다.
     *
     * @param userId 유저 UUID
     * @param petId  펫 ID
     * @param status 변경할 상태 (APPROVED 등)
     */
    void updateRegistrationStatus(@Param("userId") UUID userId,
                                  @Param("petId") Long petId,
                                  @Param("status") String status);
}
