package com.dodo.backend.userpet.service;

import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.userpet.entity.RegistrationStatus;
import com.dodo.backend.userpet.entity.UserPet;
import com.dodo.backend.userpet.repository.UserPetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link UserPetService}의 구현체로, 가족 및 멤버십 도메인의 비즈니스 로직을 수행합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserPetServiceImpl implements UserPetService {

    private final UserPetRepository userPetRepository;

    /**
     * {@inheritDoc}
     * <p>
     * User와 Pet의 ID를 조합하여 복합키를 생성하고,
     * 전달받은 상태값으로 UserPet 엔티티를 생성하여 저장합니다.
     */
    @Transactional
    @Override
    public void registerUserPet(User user, Pet pet, RegistrationStatus status) {

        UserPet.UserPetId userPetId = new UserPet.UserPetId(user.getUsersId(), pet.getPetId());

        UserPet userPet = UserPet.builder()
                .id(userPetId)
                .user(user)
                .pet(pet)
                .registrationStatus(status)
                .build();

        userPetRepository.save(userPet);
    }
}