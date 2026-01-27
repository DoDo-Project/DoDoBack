package com.dodo.backend.pet.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 반려동물(Pet)의 고유 정보(이름, 품종, 생년월일 등)를 관리하는 엔티티 클래스입니다.
 * <p>
 * 데이터베이스의 'pet' 테이블과 매핑되며, 특정 소유자(User)에 종속되지 않는
 * 독립적인 동물 개체 정보를 담고 있습니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "pet")
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pet_id")
    private Long petId;

    @Column(name = "registration_number", length = 15, nullable = false)
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex", nullable = false)
    private PetSex sex;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Column(name = "birth", nullable = false)
    private LocalDateTime birth;

    @Column(name = "pet_name", length = 50, nullable = false)
    private String petName;

    @Enumerated(EnumType.STRING)
    @Column(name = "species", nullable = false)
    private PetSpecies species;

    @Column(name = "breed", length = 30, nullable = false)
    private String breed;

    @Column(name = "reference_heart_rate")
    private Integer referenceHeartRate;

    @Column(name = "device_id", length = 255, nullable = false)
    private String deviceId;
}