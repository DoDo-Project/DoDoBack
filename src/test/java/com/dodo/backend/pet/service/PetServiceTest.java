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
import com.dodo.backend.user.exception.UserErrorCode;
import com.dodo.backend.user.exception.UserException;
import com.dodo.backend.user.service.UserService;
import com.dodo.backend.userpet.entity.RegistrationStatus;
import com.dodo.backend.userpet.entity.UserPet;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
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
    private UserService userService;

    @Mock
    private UserPetService userPetService;

    @Mock
    private PetMapper petMapper;

    /**
     * 펫 등록 성공 시나리오를 테스트합니다.
     * <p>
     * 유효한 사용자 ID와 펫 정보를 전달했을 때, 펫이 정상적으로 저장되고 유저와의 관계(UserPet)가 생성되는지 검증합니다.
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

        given(petRepository.existsByRegistrationNumber(request.getRegistrationNumber())).willReturn(false);
        given(petRepository.save(any(Pet.class))).willReturn(savedPet);

        // when
        PetResponse.PetRegisterResponse response = petService.registerPet(userId, request);

        // then
        assertNotNull(response);
        assertEquals(1L, response.getPetId());

        verify(userService, times(1)).validateUserExists(userId);
        verify(petRepository, times(1)).save(any(Pet.class));
        verify(userPetService, times(1)).registerUserPet(userId, savedPet, RegistrationStatus.APPROVED);
    }

    /**
     * 존재하지 않는 유저 ID로 펫 등록을 시도할 때 예외 발생을 테스트합니다.
     * <p>
     * {@link UserErrorCode#USER_NOT_FOUND} 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("펫 등록 실패: 존재하지 않는 유저 ID인 경우 예외가 발생한다.")
    void registerPet_Fail_UserNotFound() {

        // given
        UUID userId = UUID.randomUUID();
        PetRequest.PetRegisterRequest request = PetRequest.PetRegisterRequest.builder().build();

        willThrow(new UserException(UserErrorCode.USER_NOT_FOUND))
                .given(userService).validateUserExists(userId);

        // when
        UserException exception = assertThrows(UserException.class, () ->
                petService.registerPet(userId, request)
        );

        // then
        assertEquals(UserErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    /**
     * 이미 등록된 펫 등록번호로 등록을 시도할 때 예외 발생을 테스트합니다.
     * <p>
     * {@link PetErrorCode#REGISTRATION_NUMBER_DUPLICATED} 예외가 발생하며, 저장 로직이 호출되지 않는지 검증합니다.
     */
    @Test
    @DisplayName("펫 등록 실패: 이미 존재하는 등록번호인 경우 예외가 발생한다.")
    void registerPet_Fail_DuplicateRegistrationNumber() {

        // given
        UUID userId = UUID.randomUUID();
        PetRequest.PetRegisterRequest request = PetRequest.PetRegisterRequest.builder()
                .registrationNumber("1234567890")
                .build();

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
     * 반려동물 정보 수정 성공 시나리오를 테스트합니다. (등록번호 변경 포함)
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
        assertEquals("NEW-999", response.getRegistrationNumber());
        assertEquals("FEMALE", response.getSex());

        verify(petMapper, times(1)).updatePetProfileInfo(request, petId);
    }

    /**
     * 기존 등록번호가 null인 상태에서 새로운 번호로 수정할 때 NPE가 발생하지 않는지 테스트합니다.
     */
    @Test
    @DisplayName("펫 수정 성공: 기존 등록번호가 null이어도 Objects.equals 덕분에 NPE 없이 수정된다.")
    void updatePet_Success_WhenOldRegistrationNumberIsNull() {

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

        // when
        petService.updatePet(petId, request);

        // then
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
     * 변경하려는 등록번호가 이미 다른 펫에게 등록되어 있을 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("펫 수정 실패: 변경하려는 등록번호가 중복인 경우 예외가 발생한다.")
    void updatePet_Fail_DuplicateRegistrationNumber() {

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

        // when
        PetException exception = assertThrows(PetException.class, () ->
                petService.updatePet(petId, request)
        );

        // then
        assertEquals(PetErrorCode.REGISTRATION_NUMBER_DUPLICATED, exception.getErrorCode());
        verify(petMapper, times(0)).updatePetProfileInfo(any(), any());
    }

    /**
     * 가족 초대 코드 발급 성공 시나리오를 테스트합니다.
     * <p>
     * 1. 펫 존재 여부를 확인합니다.<br>
     * 2. UserPetService를 통해 코드 발급 로직이 호출되는지 검증합니다.<br>
     * 3. 반환된 DTO에 코드와 만료시간(expiresIn)이 정상적으로 매핑되었는지 확인합니다.
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
     * <p>
     * {@link PetErrorCode#PET_NOT_FOUND} 예외가 발생하는지 검증합니다.
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
     * <p>
     * 1. UserPetService를 통해 검증 및 등록이 완료된 결과(Pet 엔티티, 가족 목록)를 받습니다.<br>
     * 2. 반환된 DTO에 펫 정보와 가족 구성원 목록이 올바르게 매핑되었는지 검증합니다.
     */
    @Test
    @DisplayName("가족 참여 성공: 유효한 코드로 요청 시 펫 정보와 가족 목록을 반환한다.")
    void joinFamily_Success() {

        // given
        UUID userId = UUID.randomUUID();
        String invitationCode = "7X9K2P";
        PetRequest.PetFamilyJoinRequest request = new PetRequest.PetFamilyJoinRequest(invitationCode);

        Pet pet = Pet.builder()
                .petId(100L)
                .petName("보리")
                .build();

        com.dodo.backend.user.entity.User user1 = com.dodo.backend.user.entity.User.builder()
                .usersId(UUID.randomUUID())
                .nickname("아빠")
                .profileUrl("http://img1.com")
                .build();

        com.dodo.backend.user.entity.User user2 = com.dodo.backend.user.entity.User.builder()
                .usersId(userId)
                .nickname("나")
                .profileUrl("http://img2.com")
                .build();

        UserPet member1 = UserPet.builder().user(user1).build();
        UserPet member2 = UserPet.builder().user(user2).build();

        List<UserPet> familyMembers = List.of(member1, member2);

        // UserPetService의 리턴값 (Map 대신 DTO/Record를 쓰기로 했지만, 현재 코드 기준으로는 Map일 수 있음.
        // 만약 DTO 리팩토링 전이라면 Map, 후라면 객체에 맞게 수정 필요. 여기선 Map 기준으로 작성)
        Map<String, Object> serviceResult = Map.of(
                "pet", pet,
                "members", familyMembers
        );

        given(userPetService.joinFamilyByCode(userId, invitationCode)).willReturn(serviceResult);

        // when
        PetResponse.PetFamilyJoinResponse response = petService.joinFamily(userId, request);

        // then
        assertNotNull(response);
        assertEquals(pet.getPetId(), response.getPetId());
        assertEquals(pet.getPetName(), response.getPetName());
        assertEquals(2, response.getFamilyMembers().size());
        assertEquals(user1.getUsersId(), response.getFamilyMembers().get(0).getUserId());
        assertEquals("아빠", response.getFamilyMembers().get(0).getNickname());

        verify(userPetService, times(1)).joinFamilyByCode(userId, invitationCode);
    }

    /**
     * UserPetService에서 예외 발생 시 PetService에서도 그대로 예외가 전파되는지 테스트합니다.
     * <p>
     * 예: 코드가 틀렸거나(INVITATION_NOT_FOUND), 이미 가족인 경우(ALREADY_FAMILY_MEMBER) 등
     */
    @Test
    @DisplayName("가족 참여 실패: 하위 서비스에서 예외 발생 시 그대로 전파된다.")
    void joinFamily_Fail_PropagateException() {

        // given
        UUID userId = UUID.randomUUID();
        String invalidCode = "INVALID";
        PetRequest.PetFamilyJoinRequest request = new PetRequest.PetFamilyJoinRequest(invalidCode);

        willThrow(new com.dodo.backend.userpet.exception.UserPetException(
                com.dodo.backend.userpet.exception.UserPetErrorCode.INVITATION_NOT_FOUND)
        ).given(userPetService).joinFamilyByCode(userId, invalidCode);

        // when & then
        assertThrows(com.dodo.backend.userpet.exception.UserPetException.class, () ->
                petService.joinFamily(userId, request)
        );

        verify(userPetService, times(1)).joinFamilyByCode(userId, invalidCode);
    }
}