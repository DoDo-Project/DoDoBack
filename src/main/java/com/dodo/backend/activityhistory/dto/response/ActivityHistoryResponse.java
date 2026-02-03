package com.dodo.backend.activityhistory.dto.response;

import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.activityhistory.entity.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 활동기록 도메인과 관련된 응답 데이터를 캡슐화하는 DTO 그룹 클래스입니다.
 */
@Schema(description = "활동기록 관련 응답 DTO 그룹")
public class ActivityHistoryResponse {

    /**
     * 활동 기록 생성 성공 시 반환되는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "활동 기록 생성 성공 응답")
    public static class ActivityCreateResponse {

        @Schema(description = "응답 메시지", example = "회원가입이 완료되었습니다.")
        private String message;

        @Schema(description = "생성된 활동 기록 ID", example = "1234")
        private Long historyId;

        @Schema(description = "활동 유형", example = "WALKING")
        private ActivityType activityType;

        /**
         * {@link ActivityHistory} 엔티티를 생성 응답 DTO로 변환합니다.
         *
         * @param activityHistory 변환할 활동 기록 엔티티
         * @return 변환된 ActivityCreateResponse 객체
         */
        public static ActivityCreateResponse toDto(ActivityHistory activityHistory, String message) {
            return ActivityCreateResponse.builder()
                    .message(message)
                    .historyId(activityHistory.getHistoryId())
                    .activityType(activityHistory.getActivityType())
                    .build();
        }
    }
}