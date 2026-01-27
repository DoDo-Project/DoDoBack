package com.dodo.backend.pet.controller;

import com.dodo.backend.common.exception.ErrorResponse;
import com.dodo.backend.pet.dto.request.PetRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetRegisterRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetUpdateRequest;
import com.dodo.backend.pet.dto.response.PetResponse;
import com.dodo.backend.pet.dto.response.PetResponse.PetRegisterResponse;
import com.dodo.backend.pet.dto.response.PetResponse.PetUpdateResponse;
import com.dodo.backend.pet.service.PetService;
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
}