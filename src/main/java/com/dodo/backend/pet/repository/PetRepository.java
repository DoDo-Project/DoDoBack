package com.dodo.backend.pet.repository;

import com.dodo.backend.pet.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@link Pet} 엔티티의 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 * <p>
 * 펫 정보의 조회, 저장, 수정, 삭제 기능을 제공하며,
 * 등록번호 중복 검사 등의 도메인 특화 쿼리를 포함합니다.
 */
@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {

    /**
     * 주어진 등록번호와 일치하는 펫 정보가 존재하는지 확인합니다.
     *
     * @param registrationNumber 중복 검사할 반려동물 등록번호
     * @return 등록번호가 존재하면 true, 그렇지 않으면 false
     */
    boolean existsByRegistrationNumber(String registrationNumber);

    /**
     * 디바이스 ID 중복 여부 확인
     * @param deviceId 확인할 디바이스 ID
     * @return 존재하면 true, 없으면 false
     */
    boolean existsByDeviceId(String deviceId);

    /**
     * 디바이스 ID를 기반으로 반려동물 엔티티를 조회합니다.
     *
     * @param deviceId 조회할 디바이스 고유 ID
     * @return 펫 엔티티 (Optional)
     */
    Optional<Pet> findByDeviceId(String deviceId);
}