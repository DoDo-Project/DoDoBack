package com.dodo.backend.pet.service;

import com.dodo.backend.pet.dto.request.PetRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetRegisterRequest;
import com.dodo.backend.pet.dto.response.PetResponse;
import com.dodo.backend.pet.dto.response.PetResponse.PetRegisterResponse;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.exception.PetErrorCode;
import com.dodo.backend.pet.exception.PetException;
import com.dodo.backend.pet.repository.PetRepository;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.exception.UserErrorCode;
import com.dodo.backend.user.exception.UserException;
import com.dodo.backend.user.repository.UserRepository;
import com.dodo.backend.userpet.entity.RegistrationStatus;
import com.dodo.backend.userpet.service.UserPetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
    private final UserRepository userRepository;
    private final UserPetService userPetService; // Repository 대신 Service 주입

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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        if (petRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new PetException(PetErrorCode.REGISTRATION_NUMBER_DUPLICATED);
        }

        Pet pet = request.toEntity();
        Pet savedPet = petRepository.save(pet);

        userPetService.registerUserPet(user, savedPet, RegistrationStatus.APPROVED);

        log.info("펫 등록 및 유저 관계 설정 완료 - User: {}, PetId: {}", userId, savedPet.getPetId());

        return PetRegisterResponse.toDto(savedPet.getPetId(), "새 반려동물을 등록 완료했습니다.");
    }
}