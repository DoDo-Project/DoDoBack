package com.dodo.backend.activityhistory.controller;

import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest;
import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityCreateRequest;
import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityStartRequest;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse.ActivityCreateResponse;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse.ActivitySimpleResponse;
import com.dodo.backend.activityhistory.service.ActivityHistoryService;
import com.dodo.backend.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Activity History API", description = "활동 기록 관련 API")
@RequestMapping("/activities/history")
@Slf4j
public class ActivityHistoryController {

    private final ActivityHistoryService activityHistoryService;

    /**
     * 새로운 활동(산책, 수면 등) 기록을 생성합니다.
     * <p>
     * 이 API는 활동의 '시작 전' 단계에서 호출되며, DB에 초기 상태(BEFORE)로 기록을 생성합니다.
     * 실제 활동 시작은 추후 별도의 API를 통해 처리될 수 있습니다.
     *
     * @param userDetails SecurityContext에서 추출한 인증 객체 (로그인한 유저)
     * @param request     생성할 활동 정보(반려동물 ID, 활동 유형)가 담긴 요청 DTO
     * @return 생성된 활동 기록의 ID와 유형이 포함된 응답 객체 (HTTP 201 Created)
     */
    @Operation(summary = "활동 기록 생성",
            description = "새로운 활동 기록을 생성합니다. 초기 상태는 '시작 전(BEFORE)'으로 설정됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "활동 기록이 성공적으로 생성되었습니다.",
                    content = @Content(schema = @Schema(implementation = ActivityCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "활동을 기록할 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"활동을 기록할 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "진행 중인 활동 기록이 이미 존재합니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "409 Conflict", value = "{\"status\": 409, \"message\": \"진행 중인 활동 기록이 이미 존재합니다.\"}"))),
            @ApiResponse(responseCode = "429", description = "잠시 후 다시 시도해주세요.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "429 Too Many Requests", value = "{\"status\": 429, \"message\": \"잠시 후 다시 시도해주세요.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping
    public ResponseEntity<ActivityCreateResponse> createActivityHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid ActivityCreateRequest request
    ) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("활동 기록 생성 요청 - User: {}, PetId: {}, Type: {}", userId, request.getPetId(), request.getActivityType());

        ActivityCreateResponse response = activityHistoryService.createActivity(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 생성된 활동 기록을 시작(IN_PROGRESS)합니다.
     * <p>
     * 활동 상태가 '시작 전(BEFORE)'에서 '진행 중(IN_PROGRESS)'으로 변경되며,
     * 시작 시간과 요청된 위치(GPS) 정보가 기록됩니다.
     * </p>
     *
     * @param historyId   활동 기록 ID (Path Variable)
     * @param userDetails 인증 객체
     * @param request     시작 위치 정보(위도, 경도) DTO
     * @return 성공 메시지가 담긴 DTO
     */
    @Operation(summary = "활동 시작",
            description = "대기 중(BEFORE)인 활동 기록을 시작(IN_PROGRESS)합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "활동 기록이 시작되었습니다.",
                    content = @Content(schema = @Schema(implementation = ActivitySimpleResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "활동을 시작할 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"활동을 시작할 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 활동 기록을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"해당 활동 기록을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "이미 활동기록이 진행중입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "409 Conflict", value = "{\"status\": 409, \"message\": \"이미 활동기록이 진행중입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PatchMapping("/{historyId}/start")
    public ResponseEntity<ActivitySimpleResponse> startActivity(
            @PathVariable Long historyId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid ActivityStartRequest request
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("활동 시작 요청 - User: {}, HistoryId: {}, Lat: {}, Lon: {}",
                userId, historyId, request.getStartLatitude(), request.getStartLongitude());

        activityHistoryService.startActivity(userId, historyId, request);

        return ResponseEntity.ok(ActivitySimpleResponse.toDto("활동 기록이 시작되었습니다."));
    }
}