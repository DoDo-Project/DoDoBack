package com.dodo.backend.userpet.repository;

import com.dodo.backend.userpet.entity.UserPet;
import com.dodo.backend.userpet.entity.UserPet.UserPetId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * {@link UserPet} 엔티티의 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 * <p>
 * 유저와 펫 사이의 관계 데이터 생성 및 관리를 수행합니다.
 */
@Repository
public interface UserPetRepository extends JpaRepository<UserPet, UserPetId> {

    @Query("SELECT up FROM UserPet up JOIN FETCH up.user WHERE up.pet.petId = :petId")
    List<UserPet> findAllByPetId(@Param("petId") Long petId);
}