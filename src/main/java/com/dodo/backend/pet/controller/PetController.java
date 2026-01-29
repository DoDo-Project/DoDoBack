package com.dodo.backend.pet.controller;

import com.dodo.backend.common.exception.ErrorResponse;
import com.dodo.backend.pet.dto.request.PetRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetFamilyJoinRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetRegisterRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetUpdateRequest;
import com.dodo.backend.pet.dto.response.PetResponse;
import com.dodo.backend.pet.dto.response.PetResponse.*;
import com.dodo.backend.pet.service.PetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 반려동물 도메인의 HTTP 요청을 처리하는 컨트롤러 클래스입니다.
 * <p>
 * 클라이언트로부터 펫 등록, 조회 등의 요청을 받아 서비스 계층으로 전달하고,
 * 처리 결과를 응답으로 반환합니다.
 */
@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
@Tag(name = "Pets API", description = "반려동물 관련 API")
@Slf4j
public class PetController {

    private final PetService petService;

    /**
     * 새로운 반려동물을 등록하고 사용자와 연결합니다.
     * <p>
     * 인증된 사용자 정보를 기반으로 펫 등록을 수행하며,
     * 성공 시 201 Created 상태 코드와 함께 생성된 펫의 ID를 반환합니다.
     *
     * @param request     펫 등록에 필요한 상세 정보 (이름, 품종, 등록번호 등)
     * @param userDetails Spring Security를 통해 인증된 사용자의 세부 정보
     * @return 등록된 펫의 ID를 포함한 응답 객체 (HTTP 201)
     */
    @Operation(summary = "펫 생성", description = "새로운 반려동물을 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "새 반려동물을 등록 완료했습니다.",
                    content = @Content(schema = @Schema(implementation = PetRegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 등록번호입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "409 Conflict", value = "{\"status\": 409, \"message\": \"이미 존재하는 등록번호입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping
    public ResponseEntity<PetRegisterResponse> createPet(
            @Valid @RequestBody PetRegisterRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("펫 등록 요청 수신 - User: {}, PetName: {}", userId, request.getPetName());

        PetRegisterResponse response = petService.registerPet(userId, request);

        return ResponseEntity.status(201).body(response);
    }

    /**
     * 기존 반려동물의 정보를 수정합니다.
     * <p>
     * 변경이 필요한 필드만 선택적으로 수정할 수 있으며,
     * 요청한 사용자가 해당 반려동물의 소유자인지 확인 후 처리를 완료합니다.
     *
     * @param petId       수정할 반려동물의 고유 ID
     * @param request     수정할 반려동물 정보 (성별, 나이, 이름 등)
     * @param userDetails Spring Security를 통해 인증된 사용자의 세부 정보
     * @return 수정 완료 메시지와 최신 정보가 담긴 응답 객체 (HTTP 200)
     */
    @Operation(summary = "펫 정보 수정", description = "기존에 등록된 반려동물의 프로필을 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "반려동물 정보가 성공적으로 수정되었습니다.",
                    content = @Content(schema = @Schema(implementation = PetUpdateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "자신이 등록한 반려동물만 수정할 수 있습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"자신이 등록한 반려동물만 수정할 수 있습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 반려동물을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"해당 ID의 반려동물을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PatchMapping("/{petId}")
    public ResponseEntity<PetUpdateResponse> updatePet(
            @PathVariable Long petId,
            @Valid @RequestBody PetUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("펫 정보 수정 요청 수신 - PetId: {}, User: {}", petId, userId);

        PetUpdateResponse response = petService.updatePet(petId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * 가족 초대 코드를 생성합니다.
     * <p>
     * 해당 반려동물에 가족 구성원을 초대하기 위한 6자리 코드를 발급합니다.
     * 발급된 코드는 <b>15분간 유효</b>하며, 유효 기간 내에는 <b>중복 발급되지 않습니다.</b>
     *
     * @param petId 초대 코드를 생성할 반려동물의 ID
     * @param userDetails 인증된 사용자 정보
     * @return 생성된 초대 코드와 만료 시간(초 단위) 정보를 담은 응답 객체
     */
    @Operation(summary = "가족 초대 코드 생성",
            description = "반려동물에 가족을 초대하기 위한 코드를 생성하고" +
                    "생성된 코드는 15분간 유효하며, 유효 기간 내에는 중복 발급되지 않습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "초대 코드가 생성되었습니다.",
                    content = @Content(schema = @Schema(implementation = PetInvitationResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "자신이 등록하거나 속해있는 반려동물만 초대할 수 있습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"자신이 등록하거나 속해있는 반려동물만 초대할 수 있습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 반려동물을 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"해당 ID의 반려동물을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "이미 유효한 초대 코드가 존재합니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "409 Conflict", value = "{\"status\": 409, \"message\": \"이미 유효한 초대 코드가 존재합니다. 만료 후 다시 시도해주세요.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping("/{petId}/invitation-code")
    public ResponseEntity<PetInvitationResponse> createInvitationCode(
            @PathVariable Long petId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());

        return ResponseEntity.ok(petService.issueInvitationCode(userId, petId));
    }

    /**
     * 초대 코드를 입력하여 반려동물 가족 그룹에 참여합니다.
     * <p>
     * 유효한 코드인 경우 즉시 가족(APPROVED)으로 등록되며,
     * 해당 반려동물의 정보와 현재 가족 구성원 목록을 응답합니다.
     *
     * @param request     6자리 초대 코드가 담긴 요청 객체
     * @param userDetails 인증된 사용자 정보
     * @return 참여한 펫 정보와 전체 가족 구성원 목록 (HTTP 200)
     */
    @Operation(summary = "가족 초대 수락", description = "초대 코드를 입력하여 해당 반려동물의 가족으로 등록됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "가족 등록 성공",
                    content = @Content(schema = @Schema(implementation = PetFamilyJoinResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "만료되었거나 존재하지 않는 초대 코드입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"만료되었거나 존재하지 않는 초대 코드입니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "이미 가족으로 등록되어있습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "409 Conflict", value = "{\"status\": 409, \"message\": \"이미 가족으로 등록되어있습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping("/family")
    public ResponseEntity<PetFamilyJoinResponse> joinFamily(
            @Valid @RequestBody PetFamilyJoinRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("가족 초대 수락 요청 - User: {}, Code: {}", userId, request.getCode());

        return ResponseEntity.ok(petService.joinFamily(userId, request));
    }

    /**
     * 로그인한 사용자의 반려동물 목록을 페이징하여 조회합니다.
     * <p>
     * 각 반려동물의 기본 정보(이름, 종, 품종 등)와 최신 체중 데이터를 포함합니다.
     * 페이지 번호(page)는 0부터 시작하며, 한 페이지당 기본 10개의 데이터를 반환합니다.
     *
     * @param pageable    페이징 정보 (page, size, sort)
     * @param userDetails 인증된 사용자 정보
     * @return 페이징된 반려동물 목록과 페이지 메타데이터 (HTTP 200)
     */
    @Operation(summary = "반려동물 목록 조회", description = "로그인한 사용자의 반려동물 목록을 페이징하여 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PetListResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"사용자를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/list")
    public ResponseEntity<PetListResponse> getPetList(
            @Parameter(hidden = true) @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        PetListResponse response = petService.getPetList(userId, pageable);

        return ResponseEntity.ok(response);
    }
}