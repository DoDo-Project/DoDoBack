package com.dodo.backend.userpet.entity;

import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 회원(User)과 반려동물(Pet) 간의 N:M 관계를 해소하고,
 * 등록 상태 및 시점 정보를 관리하는 연결 엔티티 클래스입니다.
 * <p>
 * 데이터베이스의 'users_animals' 테이블과 매핑되며,
 * 복합키(UserPetId)를 사용하여 식별 관계를 구성합니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users_animals")
public class UserPet {

    @EmbeddedId
    private UserPetId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("petId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false)
    private RegistrationStatus registrationStatus;

    @CreatedDate
    @Column(name = "registration_created_at", nullable = false, updatable = false)
    private LocalDateTime registrationCreatedAt;

    @LastModifiedDate
    @Column(name = "registration_updated_at")
    private LocalDateTime registrationUpdatedAt;

    /**
     * UserPet 엔티티의 식별자를 구성하는 임베디드 복합키 클래스입니다.
     */
    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class UserPetId implements Serializable {
        private UUID userId;
        private Long petId;
    }
}