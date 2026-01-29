package com.dodo.backend.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 인증 번호 및 초대 코드를 생성하는 유틸리티 클래스입니다.
 * <p>
 * 숫자로 구성된 인증 번호는 {@link ThreadLocalRandom}을 사용하여 성능을 고려했으며,
 * 보안이 중요한 초대 코드는 {@link SecureRandom}을 사용하여 예측 불가능성을 보장합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VerificationCodeGenerator {

    private static final String INVITATION_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int INVITATION_CODE_LENGTH = 6;
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * 6자리 숫자 형태의 인증 번호를 생성합니다. (범위: 100,000 ~ 999,999)
     * <p>
     * 이메일/SMS 인증 등에 사용됩니다.
     *
     * @return 6자리의 숫자 문자열
     */
    public static String generateCode() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(code);
    }

    /**
     * 6자리 영문 대문자와 숫자가 혼합된 가족 초대 코드를 생성합니다.
     * <p>
     * 예: "7X9K2P", "A1B2C3"
     *
     * @return 6자리의 초대 코드 문자열
     */
    public static String generateInvitationCode() {
        StringBuilder sb = new StringBuilder(INVITATION_CODE_LENGTH);
        for (int i = 0; i < INVITATION_CODE_LENGTH; i++) {
            int randomIndex = secureRandom.nextInt(INVITATION_CHARACTERS.length());
            sb.append(INVITATION_CHARACTERS.charAt(randomIndex));
        }
        return sb.toString();
    }
}