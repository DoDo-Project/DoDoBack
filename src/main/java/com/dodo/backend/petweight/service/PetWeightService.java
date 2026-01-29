package com.dodo.backend.petweight.service;

import java.util.List;
import java.util.Map;

/**
 * 반려동물 체중 도메인의 비즈니스 로직을 담당하는 서비스 인터페이스입니다.
 */
public interface PetWeightService {

    /**
     * 여러 반려동물의 현재(가장 최근) 체중을 일괄 조회합니다.
     *
     * @param petIds 체중을 조회할 반려동물들의 ID 목록
     * @return 펫 ID를 Key로, 최근 체중(kg)을 Value로 가지는 Map 객체 (기록이 없는 펫은 포함되지 않음)
     */
    Map<Long, Double> getRecentWeights(List<Long> petIds);
}