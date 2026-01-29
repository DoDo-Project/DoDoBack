package com.dodo.backend.pet.service;

import com.dodo.backend.pet.dto.request.PetRequest.PetFamilyJoinRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetRegisterRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetUpdateRequest;
import com.dodo.backend.pet.dto.response.PetResponse.PetFamilyJoinResponse;
import com.dodo.backend.pet.dto.response.PetResponse.PetFamilyJoinResponse.FamilyMemberList;
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
import com.dodo.backend.userpet.entity.UserPet;
import com.dodo.backend.userpet.service.UserPetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.dodo.backend.pet.exception.PetErrorCode.PET_NOT_FOUND;
import static com.dodo.backend.pet.exception.PetErrorCode.REGISTRATION_NUMBER_DUPLICATED;

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
     * 사용자의 요청 정보를 기반으로 반려동물을 등록하고, 소유자 관계를 설정합니다.
     * <p>
     * <b>처리 과정:</b>
     * <ol>
     * <li>사용자 ID(UUID)의 유효성을 검사합니다. (존재하지 않는 경우 예외 발생)</li>
     * <li>요청된 등록번호가 이미 존재하는지 중복 여부를 확인합니다.</li>
     * <li>요청 DTO를 {@link Pet} 엔티티로 변환하여 데이터베이스에 저장합니다.</li>
     * <li>{@link UserPetService}를 호출하여 등록한 사용자를 해당 펫의 소유자(APPROVED)로 설정합니다.</li>
     * </ol>
     *
     * @param userId  펫을 등록하는 사용자의 UUID
     * @param request 펫 이름, 품종, 등록번호 등 상세 정보가 담긴 요청 객체
     * @return 생성된 펫의 ID와 성공 메시지를 포함한 응답 객체
     * @throws UserException 사용자를 찾을 수 없는 경우
     * @throws PetException  이미 존재하는 등록번호인 경우 (REGISTRATION_NUMBER_DUPLICATED)
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
     * 기존 반려동물의 프로필 정보를 수정합니다.
     * <p>
     * <b>처리 과정:</b>
     * <ol>
     * <li>{@link #getPet(Long)}을 통해 수정할 반려동물 엔티티를 조회합니다.</li>
     * <li>등록번호 변경 요청이 있는 경우, 해당 번호의 중복 여부를 검사합니다.</li>
     * <li>MyBatis Mapper를 호출하여 값이 존재하는 필드만 동적으로 업데이트(Dynamic Update)합니다.</li>
     * <li>수정 완료된 최신 정보를 포함한 응답 객체를 반환합니다.</li>
     * </ol>
     *
     * @param petId   수정할 반려동물의 ID
     * @param request 변경할 필드(이름, 나이, 체중 등)만 포함된 수정 요청 객체
     * @return 수정된 반려동물의 상세 정보를 담은 응답 객체
     * @throws PetException 해당 ID의 반려동물이 없거나, 변경하려는 등록번호가 이미 존재하는 경우
     */
    @Transactional
    @Override
    public PetUpdateResponse updatePet(Long petId, PetUpdateRequest request) {

        Pet pet = getPet(petId);

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
     * 가족 초대를 위한 1회용 인증 코드를 생성합니다.
     * <p>
     * <b>처리 과정:</b>
     * <ol>
     * <li>{@link #getPet(Long)}을 호출하여 대상 반려동물의 존재 여부를 우선 검증합니다.</li>
     * <li>실제 코드 생성 및 Redis 저장 로직은 {@link UserPetService}에 위임합니다.</li>
     * <li>생성된 6자리 코드와 유효 시간을 반환받아 응답 객체로 변환합니다.</li>
     * </ol>
     *
     * @param userId 코드를 요청한 사용자의 ID (권한 검증용)
     * @param petId  초대할 반려동물의 ID
     * @return 생성된 초대 코드와 만료 시간(초 단위)
     * @throws PetException 반려동물이 존재하지 않는 경우
     */
    @Transactional(readOnly = true)
    @Override
    public PetInvitationResponse issueInvitationCode(UUID userId, Long petId) {

        getPet(petId);

        Map<String, Object> result = userPetService.generateInvitationCode(userId, petId);

        return PetInvitationResponse.builder()
                .code((String) result.get("code"))
                .expiresIn((Long) result.get("expiresIn"))
                .build();
    }

    /**
     * 초대 코드를 입력하여 사용자를 가족 구성원으로 등록합니다.
     * <p>
     * <b>처리 과정:</b>
     * <ol>
     * <li>{@link UserPetService#joinFamilyByCode}를 호출하여 코드 검증 및 멤버십 등록을 수행합니다.</li>
     * <li>반환된 결과 Map에서 펫 엔티티와 가족 구성원 목록(List&lt;UserPet&gt;)을 추출합니다.</li>
     * <li>엔티티 리스트를 클라이언트 응답용 DTO인 {@link FamilyMemberList}로 변환합니다.</li>
     * <li>최종적으로 펫 정보와 가족 목록을 포함한 응답 객체를 반환합니다.</li>
     * </ol>
     *
     * @param userId  초대 코드를 입력한 사용자의 ID
     * @param request 6자리 초대 코드가 포함된 요청 객체
     * @return 참여한 펫의 정보와 갱신된 가족 구성원 목록
     */
    @Transactional
    @Override
    public PetFamilyJoinResponse joinFamily(UUID userId, PetFamilyJoinRequest request) {

        Map<String, Object> result = userPetService.joinFamilyByCode(userId, request.getCode());

        Pet pet = (Pet) result.get("pet");

        List<UserPet> members = (List<UserPet>) result.get("members");

        List<FamilyMemberList> memberDto = members.stream()
                .map(up -> FamilyMemberList.builder()
                        .userId(up.getUser().getUsersId())
                        .nickname(up.getUser().getNickname())
                        .profileUrl(up.getUser().getProfileUrl())
                        .build())
                .collect(Collectors.toList());

        return PetFamilyJoinResponse.toDto(pet.getPetId(), pet.getPetName(), memberDto);
    }

    /**
     * ID를 기반으로 Pet 엔티티를 조회합니다.
     * <p>
     * 단순히 Repository를 호출하는 것을 넘어, 데이터가 없을 경우
     * {@link PetException}(PET_NOT_FOUND)을 발생시키는 역할을 수행합니다.
     * 다른 도메인 서비스에서 Pet 엔티티가 필요할 때 이 메소드를 사용합니다.
     *
     * @param petId 조회할 반려동물의 ID
     * @return 조회된 Pet 엔티티 (null 아님)
     * @throws PetException ID에 해당하는 반려동물이 존재하지 않을 경우
     */
    @Transactional(readOnly = true)
    @Override
    public Pet getPet(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new PetException(PET_NOT_FOUND));
    }
}