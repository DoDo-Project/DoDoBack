package com.dodo.backend.petweight.service;

import com.dodo.backend.petweight.entity.PetWeight;
import com.dodo.backend.petweight.repository.PetWeightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link PetWeightService}의 구현체로, 체중 기록 조회 및 관리 로직을 수행합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PetWeightServiceImpl implements PetWeightService {

    private final PetWeightRepository petWeightRepository;

    /**
     * {@inheritDoc}
     * <p>
     * <ol>
     * <li>입력된 ID 리스트가 비어있으면 빈 Map을 반환하여 불필요한 DB 호출을 방지합니다.</li>
     * <li>리포지토리를 호출하여 모든 대상 펫의 최신 체중 데이터를 한 번에 조회합니다. (IN 절 활용)</li>
     * <li>조회된 결과 리스트({@code Object[]})를 펫 ID를 Key로 하는 {@code Map<Long, Double>}으로 변환하여 반환합니다.</li>
     * </ol>
     */
    @Transactional(readOnly = true)
    @Override
    public Map<Long, Double> getRecentWeights(List<Long> petIds) {

        if (petIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> results = petWeightRepository.findRecentWeightsByPetIds(petIds);

        return results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Double) row[1]
                ));
    }
}