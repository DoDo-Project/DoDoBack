package com.dodo.backend.pet.controller;

import com.dodo.backend.common.exception.ErrorResponse;
import com.dodo.backend.pet.dto.request.PetRequest.PetRegisterRequest;
import com.dodo.backend.pet.dto.response.PetResponse.PetRegisterResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}