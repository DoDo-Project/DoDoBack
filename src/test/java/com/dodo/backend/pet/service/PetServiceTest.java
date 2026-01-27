package com.dodo.backend.pet.service;

import com.dodo.backend.pet.dto.request.PetRequest;
import com.dodo.backend.pet.dto.response.PetResponse;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.entity.PetSex;
import com.dodo.backend.pet.entity.PetSpecies;
import com.dodo.backend.pet.exception.PetErrorCode;
import com.dodo.backend.pet.exception.PetException;
import com.dodo.backend.pet.mapper.PetMapper;
import com.dodo.backend.pet.repository.PetRepository;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.exception.UserErrorCode;
import com.dodo.backend.user.exception.UserException;
import com.dodo.backend.user.repository.UserRepository;
import com.dodo.backend.userpet.entity.RegistrationStatus;
import com.dodo.backend.userpet.service.UserPetService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link PetService}의 비즈니스 로직을 검증하는 테스트 클래스입니다.
 * <p>
 * Mockito를 사용하여 외부 의존성(Repository 등)을 격리하고,
 * 펫 등록 성공 및 예외 발생 시나리오(유저 없음, 중복 등록 등)를 단위 테스트로 확인합니다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @InjectMocks
    private PetServiceImpl petService;

    @Mock
    private PetRepository petRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPetService userPetService;

    @Mock
    private PetMapper petMapper;

    /**
     * 펫 등록 성공 시나리오를 테스트합니다.
     * <p>
     * 유효한 사용자 ID와 펫 정보를 전달했을 때,
     * 펫이 정상적으로 저장되고 유저와의 관계(UserPet)가 생성되는지 검증합니다.
     */
    @Test
    @DisplayName("펫 등록 성공: 정상적인 요청 시 펫이 저장되고 유저와 연결된다.")
    void registerPet_Success() {
        log.info("테스트 시작: 펫 등록 성공 시나리오");

        // given
        UUID userId = UUID.randomUUID();
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "usersId", userId);

        PetRequest.PetRegisterRequest request = PetRequest.PetRegisterRequest.builder()
                .petName("바둑이")
                .species(PetSpecies.CANINE)
                .breed("Poodle")
                .age(3)
                .birth(LocalDateTime.now())
                .registrationNumber("1234567890")
                .sex(PetSex.MALE)
                .deviceId("DEV_123")
                .build();

        Pet savedPet = request.toEntity();
        ReflectionTestUtils.setField(savedPet, "petId", 1L);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(petRepository.existsByRegistrationNumber(request.getRegistrationNumber())).willReturn(false);
        given(petRepository.save(any(Pet.class))).willReturn(savedPet);

        // when
        PetResponse.PetRegisterResponse response = petService.registerPet(userId, request);

        // then
        log.info("생성된 펫 ID: {}", response.getPetId());
        assertNotNull(response);
        assertEquals(1L, response.getPetId());

        verify(petRepository, times(1)).save(any(Pet.class));
        verify(userPetService, times(1)).registerUserPet(user, savedPet, RegistrationStatus.APPROVED);

        log.info("테스트 종료: 펫 등록 성공 시나리오");
    }

    /**
     * 존재하지 않는 유저 ID로 펫 등록을 시도할 때 예외 발생을 테스트합니다.
     * <p>
     * {@link UserErrorCode#USER_NOT_FOUND} 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("펫 등록 실패: 존재하지 않는 유저 ID인 경우 예외가 발생한다.")
    void registerPet_Fail_UserNotFound() {
        log.info("테스트 시작: 펫 등록 실패 (유저 없음)");

        // given
        UUID userId = UUID.randomUUID();

        PetRequest.PetRegisterRequest request = PetRequest.PetRegisterRequest.builder()
                .petName("바둑이")
                .species(PetSpecies.CANINE)
                .breed("Poodle")
                .age(3)
                .birth(LocalDateTime.now())
                .registrationNumber("1234567890")
                .sex(PetSex.MALE)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when
        UserException exception = assertThrows(UserException.class, () ->
                petService.registerPet(userId, request)
        );

        // then
        log.info("발생한 예외 메시지: {}", exception.getMessage());
        assertEquals(UserErrorCode.USER_NOT_FOUND, exception.getErrorCode());

        log.info("테스트 종료: 펫 등록 실패 (유저 없음)");
    }

    /**
     * 이미 등록된 펫 등록번호로 등록을 시도할 때 예외 발생을 테스트합니다.
     * <p>
     * {@link PetErrorCode#REGISTRATION_NUMBER_DUPLICATED} 예외가 발생하며,
     * 저장 로직이 호출되지 않는지 검증합니다.
     */
    @Test
    @DisplayName("펫 등록 실패: 이미 존재하는 등록번호인 경우 예외가 발생한다.")
    void registerPet_Fail_DuplicateRegistrationNumber() {
        log.info("테스트 시작: 펫 등록 실패 (등록번호 중복)");

        // given
        UUID userId = UUID.randomUUID();
        User user = User.builder().build();

        PetRequest.PetRegisterRequest request = PetRequest.PetRegisterRequest.builder()
                .petName("바둑이")
                .species(PetSpecies.CANINE)
                .breed("Poodle")
                .age(3)
                .birth(LocalDateTime.now())
                .registrationNumber("1234567890")
                .sex(PetSex.MALE)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(petRepository.existsByRegistrationNumber(request.getRegistrationNumber())).willReturn(true);

        // when
        PetException exception = assertThrows(PetException.class, () ->
                petService.registerPet(userId, request)
        );

        // then
        log.info("발생한 예외 메시지: {}", exception.getMessage());
        assertEquals(PetErrorCode.REGISTRATION_NUMBER_DUPLICATED, exception.getErrorCode());
        verify(petRepository, times(0)).save(any(Pet.class));

        log.info("테스트 종료: 펫 등록 실패 (등록번호 중복)");
    }

    /**
     * 반려동물 정보 수정 성공 시나리오를 테스트합니다. (등록번호 변경 포함)
     */
    @Test
    @DisplayName("펫 수정 성공: 등록번호를 변경해도 중복이 없으면 정상적으로 수정된다.")
    void updatePet_Success_NewRegistrationNumber() {
        log.info("테스트 시작: 펫 수정 성공 (등록번호 변경)");

        // given
        Long petId = 1L;
        Pet existingPet = Pet.builder()
                .petId(petId)
                .registrationNumber("OLD-123")
                .petName("초코")
                .sex(PetSex.MALE)
                .build();

        PetRequest.PetUpdateRequest request = PetRequest.PetUpdateRequest.builder()
                .petName("새로운초코")
                .registrationNumber("NEW-999")
                .sex("FEMALE")
                .age(5)
                .build();

        given(petRepository.findById(petId)).willReturn(Optional.of(existingPet));
        given(petRepository.existsByRegistrationNumber("NEW-999")).willReturn(false);

        // when
        PetResponse.PetUpdateResponse response = petService.updatePet(petId, request);

        // then
        assertNotNull(response);
        assertEquals("새로운초코", response.getPetName());
        assertEquals("NEW-999", response.getRegistrationNumber());
        assertEquals("FEMALE", response.getSex());

        verify(petMapper, times(1)).updatePetProfileInfo(request, petId);
        log.info("테스트 종료: 펫 수정 성공 (등록번호 변경)");
    }

    /**
     * 기존 등록번호가 null인 상태에서 새로운 번호로 수정할 때 NPE가 발생하지 않는지 테스트합니다.
     */
    @Test
    @DisplayName("펫 수정 성공: 기존 등록번호가 null이어도 Objects.equals 덕분에 NPE 없이 수정된다.")
    void updatePet_Success_WhenOldRegistrationNumberIsNull() {
        log.info("테스트 시작: NPE 방지 로직 검증");

        // given
        Long petId = 1L;
        Pet existingPet = Pet.builder()
                .petId(petId)
                .registrationNumber(null)
                .sex(PetSex.MALE)
                .petName("기존초코")
                .species(PetSpecies.CANINE)
                .build();

        PetRequest.PetUpdateRequest request = PetRequest.PetUpdateRequest.builder()
                .registrationNumber("NEW-123")
                .build();

        given(petRepository.findById(petId)).willReturn(Optional.of(existingPet));
        given(petRepository.existsByRegistrationNumber("NEW-123")).willReturn(false);

        // when & then
        assertDoesNotThrow(() -> petService.updatePet(petId, request));

        verify(petMapper, times(1)).updatePetProfileInfo(request, petId);
        log.info("테스트 종료: NPE 방지 로직 검증 완료");
    }

    /**
     * 존재하지 않는 반려동물 ID로 수정을 시도할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("펫 수정 실패: 존재하지 않는 펫 ID인 경우 예외가 발생한다.")
    void updatePet_Fail_NotFound() {
        log.info("테스트 시작: 펫 수정 실패 (펫 없음)");

        // given
        Long petId = 999L;
        PetRequest.PetUpdateRequest request = PetRequest.PetUpdateRequest.builder().build();

        given(petRepository.findById(petId)).willReturn(Optional.empty());

        // when & then
        PetException exception = assertThrows(PetException.class, () ->
                petService.updatePet(petId, request)
        );

        assertEquals(PetErrorCode.PET_NOT_FOUND, exception.getErrorCode());
        verify(petMapper, times(0)).updatePetProfileInfo(any(), any());
        log.info("테스트 종료: 펫 수정 실패");
    }

    /**
     * 변경하려는 등록번호가 이미 다른 펫에게 등록되어 있을 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("펫 수정 실패: 변경하려는 등록번호가 중복인 경우 예외가 발생한다.")
    void updatePet_Fail_DuplicateRegistrationNumber() {
        log.info("테스트 시작: 펫 수정 실패");

        // given
        Long petId = 1L;
        Pet existingPet = Pet.builder()
                .petId(petId)
                .registrationNumber("OLD-123")
                .build();

        PetRequest.PetUpdateRequest request = PetRequest.PetUpdateRequest.builder()
                .registrationNumber("DUPLICATE-999")
                .build();

        given(petRepository.findById(petId)).willReturn(Optional.of(existingPet));
        given(petRepository.existsByRegistrationNumber("DUPLICATE-999")).willReturn(true);

        // when & then
        PetException exception = assertThrows(PetException.class, () ->
                petService.updatePet(petId, request)
        );

        assertEquals(PetErrorCode.REGISTRATION_NUMBER_DUPLICATED, exception.getErrorCode());
        verify(petMapper, times(0)).updatePetProfileInfo(any(), any());
        log.info("테스트 종료: 펫 수정 실패");
    }
}