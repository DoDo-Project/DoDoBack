package com.dodo.backend.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Scalar API Reference 명세를 담당하는 구성 클래스입니다.
 * <p>
 * API 문서의 제목, 설명, 버전 등 기본적인 메타데이터를 정의합니다.
 */
@Configuration
public class OpenApiConfig {

    /**
     * 프로젝트의 기본 정보가 포함된 OpenAPI 빈을 등록합니다.
     *
     * @return 기본 정보가 설정된 OpenAPI 객체
     */
    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("DoDo API Docs")
                .description("DoDo 프로젝트 백엔드 서비스 API 명세서입니다.")
                .version("v0.0.1");

        OpenAPI openApi = new OpenAPI();
        openApi.setInfo(info);

        return openApi;
    }
}