package com.dodo.backend.pet.service;

import com.dodo.backend.imagefile.service.ImageFileService;
import com.dodo.backend.pet.dto.request.PetRequest;
import com.dodo.backend.pet.dto.response.PetResponse;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.entity.PetSex;
import com.dodo.backend.pet.entity.PetSpecies;
import com.dodo.backend.pet.exception.PetErrorCode;
import com.dodo.backend.pet.exception.PetException;
import com.dodo.backend.pet.mapper.PetMapper;
import com.dodo.backend.pet.repository.PetRepository;
import com.dodo.backend.petweight.service.PetWeightService;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link PetService}의 비즈니스 로직을 검증하는 테스트 클래스입니다.
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
    private PetWeightService petWeightService;

    @Mock
    private ImageFileService imageFileService;

    @Mock
    private PetMapper petMapper;

    /**
     * 펫 등록 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("펫 등록 성공: 정상적인 요청 시 펫이 저장되고 유저와 연결된다.")
    void registerPet_Success() {
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
                .deviceId("DEV_123")
                .build();

        Pet savedPet = request.toEntity();
        ReflectionTestUtils.setField(savedPet, "petId", 1L);

        given(userRepository.existsById(userId)).willReturn(true);
        given(petRepository.existsByRegistrationNumber(request.getRegistrationNumber())).willReturn(false);
        given(petRepository.save(any(Pet.class))).willReturn(savedPet);

        // when
        PetResponse.PetRegisterResponse response = petService.registerPet(userId, request);

        // then
        assertNotNull(response);
        assertEquals(1L, response.getPetId());

        verify(userRepository, times(1)).existsById(userId);
        verify(petRepository, times(1)).save(any(Pet.class));
        verify(userPetService, times(1)).registerUserPet(userId, savedPet, RegistrationStatus.APPROVED);
    }

    /**
     * 존재하지 않는 유저 ID로 펫 등록을 시도할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("펫 등록 실패: 존재하지 않는 유저 ID인 경우 예외가 발생한다.")
    void registerPet_Fail_UserNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        PetRequest.PetRegisterRequest request = PetRequest.PetRegisterRequest.builder().build();

        given(userRepository.existsById(userId)).willReturn(false);

        // when
        PetException exception = assertThrows(PetException.class, () ->
                petService.registerPet(userId, request)
        );

        // then
        assertEquals(PetErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    /**
     * 이미 등록된 펫 등록번호로 등록을 시도할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("펫 등록 실패: 이미 존재하는 등록번호인 경우 예외가 발생한다.")
    void registerPet_Fail_DuplicateRegistrationNumber() {
        // given
        UUID userId = UUID.randomUUID();
        PetRequest.PetRegisterRequest request = PetRequest.PetRegisterRequest.builder()
                .registrationNumber("1234567890")
                .build();

        given(userRepository.existsById(userId)).willReturn(true);
        given(petRepository.existsByRegistrationNumber(request.getRegistrationNumber())).willReturn(true);

        // when
        PetException exception = assertThrows(PetException.class, () ->
                petService.registerPet(userId, request)
        );

        // then
        assertEquals(PetErrorCode.REGISTRATION_NUMBER_DUPLICATED, exception.getErrorCode());
        verify(petRepository, times(0)).save(any(Pet.class));
    }

    /**
     * 반려동물 정보 수정 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("펫 수정 성공: 등록번호를 변경해도 중복이 없으면 정상적으로 수정된다.")
    void updatePet_Success_NewRegistrationNumber() {
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

        verify(petMapper, times(1)).updatePetProfileInfo(request, petId);
    }

    /**
     * 존재하지 않는 반려동물 ID로 수정을 시도할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("펫 수정 실패: 존재하지 않는 펫 ID인 경우 예외가 발생한다.")
    void updatePet_Fail_NotFound() {
        // given
        Long petId = 999L;
        PetRequest.PetUpdateRequest request = PetRequest.PetUpdateRequest.builder().build();

        given(petRepository.findById(petId)).willReturn(Optional.empty());

        // when
        PetException exception = assertThrows(PetException.class, () ->
                petService.updatePet(petId, request)
        );

        // then
        assertEquals(PetErrorCode.PET_NOT_FOUND, exception.getErrorCode());
        verify(petMapper, times(0)).updatePetProfileInfo(any(), any());
    }

    /**
     * 가족 초대 코드 발급 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("초대 코드 발급 성공: 펫이 존재하면 UserPetService를 통해 코드가 발급된다.")
    void issueInvitationCode_Success() {
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;
        String expectedCode = "ABC1234";
        Long expectedExpiresIn = 900L;

        Map<String, Object> serviceResult = Map.of(
                "code", expectedCode,
                "expiresIn", expectedExpiresIn
        );

        given(petRepository.existsById(petId)).willReturn(true);
        given(userPetService.generateInvitationCode(userId, petId)).willReturn(serviceResult);

        // when
        PetResponse.PetInvitationResponse response = petService.issueInvitationCode(userId, petId);

        // then
        assertNotNull(response);
        assertEquals(expectedCode, response.getCode());
        assertEquals(expectedExpiresIn, response.getExpiresIn());

        verify(petRepository, times(1)).existsById(petId);
        verify(userPetService, times(1)).generateInvitationCode(userId, petId);
    }

    /**
     * 존재하지 않는 펫 ID로 초대 코드를 요청할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("초대 코드 발급 실패: 펫이 존재하지 않으면 예외가 발생한다.")
    void issueInvitationCode_Fail_PetNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 999L;

        given(petRepository.existsById(petId)).willReturn(false);

        // when
        PetException exception = assertThrows(PetException.class, () ->
                petService.issueInvitationCode(userId, petId)
        );

        // then
        assertEquals(PetErrorCode.PET_NOT_FOUND, exception.getErrorCode());
        verify(userPetService, times(0)).generateInvitationCode(any(), any());
    }

    /**
     * 가족 초대 코드 입력 및 참여 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("가족 참여 신청 성공: 유효한 코드로 요청 시 펫 ID와 성공 메시지를 반환한다.")
    void applyForFamily_Success() {
        // given
        UUID userId = UUID.randomUUID();
        String invitationCode = "7X9K2P";
        Long petId = 100L;
        PetRequest.PetFamilyJoinRequest request = new PetRequest.PetFamilyJoinRequest(invitationCode);

        given(userPetService.registerByInvitation(userId, invitationCode)).willReturn(petId);

        // when
        PetResponse.PetFamilyJoinRequestResponse response = petService.applyForFamily(userId, request);

        // then
        assertNotNull(response);
        assertEquals(petId, response.getPetId());
        assertEquals("가족 등록을 신청했습니다. 승인을 기다려주세요.", response.getMessage());

        verify(userPetService, times(1)).registerByInvitation(userId, invitationCode);
    }
}