package com.dodo.backend.petweight.service;

import com.dodo.backend.petweight.repository.PetWeightRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link PetWeightService}의 비즈니스 로직을 검증하는 테스트 클래스입니다.
 * <p>
 * 반려동물 목록에 대한 최신 체중 일괄 조회 로직을 중점적으로 테스트합니다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class PetWeightServiceTest {

    @InjectMocks
    private PetWeightServiceImpl petWeightService;

    @Mock
    private PetWeightRepository petWeightRepository;

    /**
     * 펫 ID 목록을 입력받아 최신 체중을 정상적으로 조회하는 시나리오를 테스트합니다.
     * <p>
     * Repository가 반환하는 Object[] 리스트가 Map<Long, Double>로
     * 올바르게 변환되는지 검증합니다.
     */
    @Test
    @DisplayName("최신 체중 조회 성공: Pet ID 목록에 해당하는 최신 체중을 Map 형태로 반환한다.")
    void getRecentWeights_Success() {
        log.info("테스트 시작: 최신 체중 조회 성공 시나리오");

        // given
        List<Long> petIds = List.of(1L, 2L);
        log.info("요청 Pet IDs: {}", petIds);

        List<Object[]> repositoryResult = List.of(
                new Object[]{1L, 5.5},
                new Object[]{2L, 8.2}
        );
        given(petWeightRepository.findRecentWeightsByPetIds(petIds)).willReturn(repositoryResult);

        // when
        Map<Long, Double> result = petWeightService.getRecentWeights(petIds);

        // then
        log.info("조회 결과 Map: {}", result);

        assertEquals(2, result.size());
        assertEquals(5.5, result.get(1L));
        assertEquals(8.2, result.get(2L));

        verify(petWeightRepository).findRecentWeightsByPetIds(petIds);

        log.info("테스트 종료: 최신 체중 조회 성공 시나리오");
    }

    /**
     * 빈 리스트가 입력되었을 때 DB 조회를 하지 않고 빈 Map을 반환하는지 테스트합니다.
     */
    @Test
    @DisplayName("최신 체중 조회: 입력된 ID 리스트가 비어있으면 DB 조회 없이 빈 Map을 반환한다.")
    void getRecentWeights_EmptyInput() {
        log.info("테스트 시작: 최신 체중 조회 (빈 리스트)");

        // given
        List<Long> emptyPetIds = Collections.emptyList();

        // when
        Map<Long, Double> result = petWeightService.getRecentWeights(emptyPetIds);

        // then
        log.info("조회 결과 Map Size: {}", result.size());
        assertTrue(result.isEmpty());

        verify(petWeightRepository, never()).findRecentWeightsByPetIds(anyList());

        log.info("테스트 종료: 최신 체중 조회 (빈 리스트)");
    }
}