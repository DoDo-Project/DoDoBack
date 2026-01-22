package com.dodo.backend.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;


/**
 * 사용자(User) 정보를 관리하는 핵심 엔티티 클래스입니다.
 * <p>
 * 데이터베이스의 {@code users} 테이블과 매핑됩니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "users_id", columnDefinition = "BINARY(16)")
    private UUID usersId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @CreatedDate
    @Column(name = "user_created_at", nullable = false, updatable = false)
    private LocalDateTime userCreatedAt;

    @Column(name = "name", length = 10, nullable = false)
    private String name;

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "has_family")
    private Boolean hasFamily;

    @Column(name = "nickname", length = 30, nullable = false)
    private String nickname;

    @Column(name = "region", length = 20, nullable = false)
    private String region;

    @Column(name = "profile_url", length = 2048, nullable = false)
    private String profileUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false)
    private UserStatus userStatus;

    @Column(name = "user_status_updated_at")
    private LocalDateTime userStatusUpdatedAt;

    @Column(name = "suspended_end_at")
    private LocalDateTime suspendedEndAt;

    @Column(name = "notification_enabled", nullable = false)
    private Boolean notificationEnabled;

}
