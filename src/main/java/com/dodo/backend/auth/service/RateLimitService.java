package com.dodo.backend.auth.service;

/**
 * 서비스의 보안 및 안정성을 위해 API 요청 횟수를 제한하고 인증 데이터를 관리하는 인터페이스입니다.
 * <p>
 * 무차별 대입 공격(Brute Force) 방지를 위한 IP 기반 제한과
 * 리소스 남용 방지를 위한 이메일 발송 제한 및 인증 코드 저장 기능을 제공합니다.
 */
public interface RateLimitService {

    /**
     * 특정 클라이언트 IP가 현재 서비스 이용 차단(Ban) 상태인지 확인합니다.
     *
     * @param clientIp 요청자의 IP 주소
     * @return 차단 상태인 경우 {@code true}, 그렇지 않으면 {@code false}
     */
    boolean isIpBanned(String clientIp);

    /**
     * 특정 클라이언트 IP의 요청 시도 횟수를 기록(증가)하고 현재 누적 횟수를 반환합니다.
     *
     * @param clientIp 요청자의 IP 주소
     * @param windowTime 횟수 기록이 유지될 시간 (분 단위)
     * @return 증가된 후의 현재 누적 시도 횟수
     */
    Long incrementAttempt(String clientIp, int windowTime);

    /**
     * 특정 클라이언트 IP를 지정된 시간 동안 서비스 이용 차단 상태로 등록합니다.
     *
     * @param clientIp 차단할 IP 주소
     * @param banTime 차단이 유지될 시간 (분 단위)
     */
    void banIp(String clientIp, int banTime);

    /**
     * 특정 클라이언트 IP에 대해 누적된 요청 시도 횟수 기록을 삭제합니다.
     * <p>
     * 주로 IP 차단이 실행되거나 인증에 성공했을 때 초기화 용도로 사용됩니다.
     *
     * @param clientIp 초기화할 IP 주소
     */
    void deleteAttempts(String clientIp);

    /**
     * 특정 이메일 주소에 대해 메일 발송 쿨타임(1분)이 적용 중인지 확인합니다.
     *
     * @param email 발송 대상 이메일 주소
     * @return 쿨타임이 적용 중이면 {@code true}, 아니면 {@code false}
     */
    boolean isEmailCooldownActive(String email);

    /**
     * 특정 이메일 주소에 대해 메일 발송 쿨타임(1분)을 설정합니다.
     * <p>
     * 단시간 내 중복적인 메일 발송 요청을 방지하기 위해 사용됩니다.
     *
     * @param email 발송 대상 이메일 주소
     */
    void setEmailCooldown(String email);

    /**
     * 생성된 인증 코드를 유효 시간 동안 저장합니다.
     * <p>
     * 저장된 코드는 이후 사용자가 입력한 값과 비교하여 본인 인증을 수행하는 데 사용됩니다.
     *
     * @param email 수신자 이메일 (Key)
     * @param code 생성된 인증 코드 (Value)
     * @param duration 코드의 유효 유지 시간 (분 단위)
     */
    void saveVerificationCode(String email, String code, int duration);
}