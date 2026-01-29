package com.dodo.backend.userpet.service;

import com.dodo.backend.common.util.VerificationCodeGenerator;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.service.UserService;
import com.dodo.backend.userpet.entity.RegistrationStatus;
import com.dodo.backend.userpet.entity.UserPet;
import com.dodo.backend.userpet.entity.UserPet.UserPetId;
import com.dodo.backend.userpet.exception.UserPetException;
import com.dodo.backend.userpet.repository.UserPetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.dodo.backend.userpet.exception.UserPetErrorCode.INVITATION_ALREADY_EXISTS;
import static com.dodo.backend.userpet.exception.UserPetErrorCode.INVITE_PERMISSION_DENIED;

/**
 * {@link UserPetService}의 구현체로, 가족 및 멤버십 도메인의 비즈니스 로직을 수행합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserPetServiceImpl implements UserPetService {

    private final UserPetRepository userPetRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserService userService;

    private static final long EXPIRATION_MINUTES = 15;
    private static final String REDIS_CODE_KEY_PREFIX = "invitation:code:";
    private static final String REDIS_PET_KEY_PREFIX = "invitation:pet:";

    /**
     * {@inheritDoc}
     * <p>
     * User와 Pet의 ID를 조합하여 복합키를 생성하고,
     * 전달받은 상태값으로 UserPet 엔티티를 생성하여 저장합니다.
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
     * {@inheritDoc}
     * <p>
     * 1. 요청한 사용자가 해당 펫의 가족 구성원(UserPet)인지 확인하고 승인된 상태인지 검증합니다.<br>
     * 2. 해당 반려동물에 대해 이미 발급된 유효한 초대 코드가 존재하는지 확인하여 중복 발급을 방지합니다.<br>
     * 3. 6자리의 랜덤 초대 코드를 생성합니다.<br>
     * 4. Redis에 초대 코드 조회용 키(Code-PetId)와 중복 방지용 키(PetId-Code)를 각각 저장합니다.<br>
     * 5. 생성된 코드와 남은 유효 시간(초 단위)을 반환합니다.
     *
     * @throws UserPetException 가족 구성원이 아니거나 승인되지 않은 경우 (INVITE_PERMISSION_DENIED),
     * 이미 유효한 코드가 존재하는 경우 (INVITATION_ALREADY_EXISTS)
     */
    @Override
    @Transactional(readOnly = true)
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
}