package com.dodo.backend.pet.service;

import com.dodo.backend.pet.dto.request.PetRequest.PetRegisterRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetUpdateRequest;
import com.dodo.backend.pet.dto.response.PetResponse.PetInvitationResponse;
import com.dodo.backend.pet.dto.response.PetResponse.PetRegisterResponse;
import com.dodo.backend.pet.dto.response.PetResponse.PetUpdateResponse;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.exception.PetException;
import com.dodo.backend.pet.mapper.PetMapper;
import com.dodo.backend.pet.repository.PetRepository;
import com.dodo.backend.user.exception.UserException;
import com.dodo.backend.user.service.UserService;
import com.dodo.backend.userpet.entity.RegistrationStatus;
import com.dodo.backend.userpet.service.UserPetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.dodo.backend.pet.exception.PetErrorCode.*;

/**
 * {@link PetService}의 구현체로, 펫 도메인의 비즈니스 로직을 수행합니다.
 * <p>
 * 펫 정보의 유효성 검사, 엔티티 생성 및 저장, 사용자-펫 관계 설정 등의
 * 트랜잭션 처리를 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;
    private final UserPetService userPetService;
    private final UserService userService;
    private final PetMapper petMapper;

    /**
     * {@inheritDoc}
     * <p>
     * 1. 사용자 존재 여부를 확인합니다.<br>
     * 2. 등록번호 중복 여부를 검사합니다.<br>
     * 3. Pet 엔티티를 생성하여 저장합니다.<br>
     * 4. UserPetService를 호출하여 사용자와 펫의 관계를 승인(APPROVED) 상태로 설정합니다.
     *
     * @throws UserException 사용자를 찾을 수 없는 경우
     * @throws PetException  이미 존재하는 등록번호인 경우
     */
    @Transactional
    @Override
    public PetRegisterResponse registerPet(UUID userId, PetRegisterRequest request) {

        log.info("반려동물 등록 시작 - userId: {}", userId);
        userService.validateUserExists(userId);

        if (request.getRegistrationNumber() != null && !request.getRegistrationNumber().isBlank()) {
            if (petRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
                throw new PetException(REGISTRATION_NUMBER_DUPLICATED);
            }
        }

        Pet pet = request.toEntity();
        Pet savedPet = petRepository.save(pet);

        userPetService.registerUserPet(userId, savedPet, RegistrationStatus.APPROVED);

        log.info("펫 등록 및 유저 관계 설정 완료 - User: {}, PetId: {}", userId, savedPet.getPetId());

        return PetRegisterResponse.toDto(savedPet.getPetId(), "새 반려동물을 등록 완료했습니다.");
    }

    /**
     * {@inheritDoc}
     * <p>
     * 1. 수정 대상 반려동물의 존재 여부를 확인합니다.<br>
     * 2. 등록번호를 변경하려는 경우, 새로운 번호가 이미 존재하는지 중복 검사를 수행합니다.<br>
     * 3. MyBatis의 동적 쿼리를 호출하여 요청된 필드만 선택적으로 업데이트합니다.<br>
     * 4. 수정된 정보와 함께 성공 메시지를 담은 응답 DTO를 반환합니다.
     *
     * @throws PetException 해당 ID의 반려동물을 찾을 수 없거나, 변경하려는 등록번호가 중복된 경우
     */
    @Transactional
    @Override
    public PetUpdateResponse updatePet(Long petId, PetUpdateRequest request) {

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new PetException(PET_NOT_FOUND));


        if (request.getRegistrationNumber() != null && !Objects.equals(pet.getRegistrationNumber(), request.getRegistrationNumber())) {
            if (petRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
                log.warn("등록번호 중복 발생 - PetId: {}, 번호: {}", petId, request.getRegistrationNumber());
                throw new PetException(REGISTRATION_NUMBER_DUPLICATED);
            }
        }

        petMapper.updatePetProfileInfo(request, petId);

        log.info("반려동물 프로필 수정 성공 - PetId: {}", petId);

        return PetUpdateResponse.toDto(
                petId,
                "반려동물 정보 수정을 완료했습니다.",
                request.getRegistrationNumber() != null ? request.getRegistrationNumber() : pet.getRegistrationNumber(),
                request.getSex() != null ? request.getSex() : pet.getSex().name(),
                request.getAge() != null ? request.getAge() : pet.getAge(),
                request.getPetName() != null ? request.getPetName() : pet.getPetName(),
                request.getBreed() != null ? request.getBreed() : pet.getBreed(),
                request.getReferenceHeartRate() != null ? request.getReferenceHeartRate() : pet.getReferenceHeartRate(),
                request.getDeviceId() != null ? request.getDeviceId() : pet.getDeviceId()
        );
    }

    /**
     * {@inheritDoc}
     * <p>
     * 1. 반려동물 존재 여부를 우선 검증합니다. (존재하지 않을 시 예외 발생)<br>
     * 2. UserPetService로 로직을 위임하여 초대 코드를 생성합니다.<br>
     * 3. 반환받은 코드 데이터를 응답 DTO로 변환하여 반환합니다.
     */
    @Transactional(readOnly = true)
    @Override
    public PetInvitationResponse issueInvitationCode(UUID userId, Long petId) {

        if (!petRepository.existsById(petId)) {
            throw new PetException(PET_NOT_FOUND);
        }

        Map<String, Object> result = userPetService.generateInvitationCode(userId, petId);

        return PetInvitationResponse.builder()
                .code((String) result.get("code"))
                .expiresIn((Long) result.get("expiresIn"))
                .build();
    }
}