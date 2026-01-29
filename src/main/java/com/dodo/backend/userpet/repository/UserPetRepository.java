package com.dodo.backend.userpet.repository;

import com.dodo.backend.userpet.entity.UserPet;
import com.dodo.backend.userpet.entity.UserPet.UserPetId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * {@link UserPet} 엔티티의 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 * <p>
 * 유저와 펫 사이의 관계 데이터 생성 및 관리를 수행합니다.
 */
@Repository
public interface UserPetRepository extends JpaRepository<UserPet, UserPetId> {

    /**
     * 특정 펫의 모든 가족 구성원(UserPet)을 조회합니다.
     * <p>
     * N+1 문제를 방지하기 위해 User 엔티티를 Fetch Join으로 함께 가져옵니다.
     */
    @Query("SELECT up FROM UserPet up JOIN FETCH up.user WHERE up.pet.petId = :petId")
    List<UserPet> findAllByPetId(@Param("petId") Long petId);

    /**
     * 특정 사용자가 속한 반려동물 목록을 페이징하여 조회합니다.
     * <p>
     * {@code @EntityGraph}를 사용하여 연관된 Pet 엔티티를 함께 로딩(Fetch Join 효과)합니다.
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 UserPet 목록
     */
    @EntityGraph(attributePaths = "pet")
    Page<UserPet> findAllByUser_UsersId(UUID userId, Pageable pageable);
}