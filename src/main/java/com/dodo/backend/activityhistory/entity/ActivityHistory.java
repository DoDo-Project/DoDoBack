package com.dodo.backend.activityhistory.entity;

import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 반려동물의 활동 기록을 저장하는 엔티티 클래스입니다.
 * <p>
 * 산책, 수면 등의 활동에 대한 거리, 위치, 시간 정보를 포함합니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "activity_history")
public class ActivityHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "distance", precision = 10, scale = 3)
    private BigDecimal distance;

    @Column(name = "activity_history_start_at")
    private LocalDateTime activityHistoryStartAt;

    @Column(name = "activity_history_end_at")
    private LocalDateTime activityHistoryEndAt;

    @Column(name = "start_latitude", precision = 10, scale = 8)
    private BigDecimal startLatitude;

    @Column(name = "start_longitude", precision = 11, scale = 8)
    private BigDecimal startLongitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_history_status", nullable = false)
    private ActivityHistoryStatus activityHistoryStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType;

}