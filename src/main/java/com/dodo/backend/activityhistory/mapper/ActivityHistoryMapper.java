package com.dodo.backend.activityhistory.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface ActivityHistoryMapper {

    /**
     * 활동 기록을 시작 상태로 변경하고, 시작 시간(NOW()) 및 위치 정보를 기록합니다.
     *
     * @param historyId      활동 기록 ID
     * @param status         변경할 상태 (IN_PROGRESS)
     * @param startLatitude  시작 위도
     * @param startLongitude 시작 경도
     */
    void startActivity(@Param("historyId") Long historyId,
                       @Param("status") String status,
                       @Param("startLatitude") BigDecimal startLatitude,
                       @Param("startLongitude") BigDecimal startLongitude);
}
