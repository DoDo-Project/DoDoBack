package com.dodo.backend.mail.util;

import java.util.concurrent.ThreadLocalRandom;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 사용자 인증을 위한 임의의 번호를 생성하는 유틸리티 클래스입니다.
 * <p>
 * 보안성과 성능을 고려하여 {@link ThreadLocalRandom}을 사용하며,
 * 외부에서의 인스턴스화를 방지하기 위해 생성자를 제한합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VerificationCodeGenerator {

    /**
     * 6자리 숫자 형태의 인증 번호를 생성합니다. (범위: 100,000 ~ 999,999)
     * <p>
     * 생성된 번호는 문자열 타입으로 반환되어 메일 전송 및 Redis 저장에 사용됩니다.
     *
     * @return 6자리의 숫자 문자열
     */
    public static String generateCode() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(code);
    }
}