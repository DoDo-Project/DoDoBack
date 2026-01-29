package com.dodo.backend.userpet.service;

import com.dodo.backend.common.util.VerificationCodeGenerator;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.service.PetService;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.service.UserService;
import com.dodo.backend.userpet.entity.RegistrationStatus;
import com.dodo.backend.userpet.entity.UserPet;
import com.dodo.backend.userpet.entity.UserPet.UserPetId;
import com.dodo.backend.userpet.exception.UserPetException;
import com.dodo.backend.userpet.repository.UserPetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.dodo.backend.userpet.exception.UserPetErrorCode.*;

/**
 * {@link UserPetService}의 구현체로, 가족 및 멤버십 도메인의 비즈니스 로직을 수행합니다.
 */
@Service
@Slf4j
public class UserPetServiceImpl implements UserPetService {

    private final UserPetRepository userPetRepository;
    private final PetService petService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserService userService;

    private static final long EXPIRATION_MINUTES = 15;
    private static final String REDIS_CODE_KEY_PREFIX = "invitation:code:";
    private static final String REDIS_PET_KEY_PREFIX = "invitation:pet:";

    /**
     * 생성자 주입 방식을 사용하되, {@link PetService}와의 순환 참조 문제를 방지하기 위해
     * {@code @Lazy} 어노테이션을 사용하여 의존성을 지연 주입합니다.
     */
    public UserPetServiceImpl(UserPetRepository userPetRepository,
                              @Lazy PetService petService,
                              RedisTemplate<String, Object> redisTemplate,
                              UserService userService) {
        this.userPetRepository = userPetRepository;
        this.petService = petService;
        this.redisTemplate = redisTemplate;
        this.userService = userService;
    }

    /**
     * 사용자와 반려동물 간의 관계(멤버십)를 생성하고 저장합니다.
     * <p>
     * <b>처리 과정:</b>
     * <ol>
     * <li>사용자 ID로 User 엔티티를 조회합니다.</li>
     * <li>User ID와 Pet ID를 조합하여 복합키({@link UserPetId})를 생성합니다.</li>
     * <li>전달받은 상태값(APPROVED, PENDING 등)을 포함하여 {@link UserPet} 엔티티를 빌드하고 저장합니다.</li>
     * </ol>
     *
     * @param userId 관계를 맺을 사용자의 UUID
     * @param pet    관계를 맺을 반려동물 엔티티
     * @param status 초기 등록 상태
     */
    @Transactional
    @Override
    public void registerUserPet(UUID userId, Pet pet, RegistrationStatus status) {

        User user = userService.getUserEntity(userId);

        UserPetId userPetId = new UserPetId(user.getUsersId(), pet.getPetId());

        UserPet userPet = UserPet.builder()
                .id(userPetId)
                .user(user)
                .pet(pet)
                .registrationStatus(status)
                .build();

        userPetRepository.save(userPet);
    }

    /**
     * 가족 초대를 위한 코드를 생성하고 Redis에 저장합니다.
     * <p>
     * <b>처리 과정:</b>
     * <ol>
     * <li>요청한 사용자가 해당 펫의 가족 구성원인지, 그리고 '승인된(APPROVED)' 상태인지 검증합니다.</li>
     * <li>이미 발급된 유효한 초대 코드가 있는지 Redis를 통해 확인하여 중복 발급을 방지합니다.</li>
     * <li>6자리의 랜덤 초대 코드를 생성합니다.</li>
     * <li>초대 코드 검증을 위한 키(code:petId)와 중복 방지용 키(petId:code)를 Redis에 저장합니다. (유효기간 15분)</li>
     * </ol>
     *
     * @param userId 요청한 사용자의 UUID
     * @param petId  초대 코드를 생성할 반려동물 ID
     * @return 생성된 코드와 만료 시간이 담긴 Map
     * @throws UserPetException 권한이 없거나 이미 코드가 존재하는 경우
     */
    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> generateInvitationCode(UUID userId, Long petId) {

        UserPet userPet = userPetRepository.findById(new UserPet.UserPetId(userId, petId))
                .orElseThrow(() -> new UserPetException(INVITE_PERMISSION_DENIED));

        if (userPet.getRegistrationStatus() != RegistrationStatus.APPROVED) {
            throw new UserPetException(INVITE_PERMISSION_DENIED);
        }

        String petKey = REDIS_PET_KEY_PREFIX + petId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(petKey))) {
            throw new UserPetException(INVITATION_ALREADY_EXISTS);
        }

        String code = VerificationCodeGenerator.generateInvitationCode();
        long expiresIn = EXPIRATION_MINUTES * 60;

        redisTemplate.opsForValue().set(
                REDIS_CODE_KEY_PREFIX + code,
                String.valueOf(petId),
                Duration.ofMinutes(EXPIRATION_MINUTES)
        );

        redisTemplate.opsForValue().set(
                petKey,
                code,
                Duration.ofMinutes(EXPIRATION_MINUTES)
        );

        log.info("가족 초대 코드 생성 완료 - PetId: {}, User: {}, Code: {}", petId, userId, code);

        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("expiresIn", expiresIn);

        return result;
    }

    /**
     * 초대 코드를 통해 가족 구성원으로 등록합니다.
     * <p>
     * <b>처리 과정:</b>
     * <ol>
     * <li>Redis에서 입력된 코드를 조회하여 유효성을 검증하고, 매핑된 Pet ID를 가져옵니다.</li>
     * <li>사용자가 이미 해당 펫의 가족으로 등록되어 있는지 중복 여부를 확인합니다.</li>
     * <li>{@link PetService#getPet}을 호출하여 Pet 엔티티를 조회합니다. (직접 리포지토리 접근 X)</li>
     * <li>{@link #registerUserPet}을 호출하여 사용자를 승인(APPROVED) 상태로 등록합니다.</li>
     * <li>갱신된 가족 구성원 목록을 DB에서 조회하여 반환합니다.</li>
     * </ol>
     *
     * @param userId 코드를 입력한 사용자의 UUID
     * @param code   사용자가 입력한 6자리 코드
     * @return 펫 엔티티와 가족 목록이 담긴 Map
     * @throws UserPetException 코드가 유효하지 않거나, 이미 가족인 경우
     */
    @Transactional
    @Override
    public Map<String, Object> joinFamilyByCode(UUID userId, String code) {

        String petIdStr = (String) redisTemplate.opsForValue().get(REDIS_CODE_KEY_PREFIX + code);

        if (petIdStr == null) {
            throw new UserPetException(INVITATION_NOT_FOUND);
        }

        Long petId = Long.valueOf(petIdStr);

        if (userPetRepository.existsById(new UserPetId(userId, petId))) {
            throw new UserPetException(ALREADY_FAMILY_MEMBER);
        }

        Pet pet = petService.getPet(petId);

        this.registerUserPet(userId, pet, RegistrationStatus.APPROVED);
        log.info("가족 초대 코드 수락 완료 - User: {}, PetId: {}", userId, petId);

        List<UserPet> familyMembers = userPetRepository.findAllByPetId(petId);

        Map<String, Object> result = new HashMap<>();
        result.put("pet", pet);
        result.put("members", familyMembers);

        return result;
    }
}