package com.dodo.backend.mail.service;

/**
 * 시스템 내 메일 발송 기능을 담당하는 서비스 인터페이스입니다.
 * <p>
 * 계정 탈퇴와 같은 본인 확인 절차가 필요한 시점에 인증 번호를 생성하고,
 * 이를 사용자 이메일로 발송하여 인증 프로세스를 지원하는 기능을 정의합니다.
 */
public interface MailService {

    /**
     * 계정 탈퇴 본인 확인을 위한 인증 번호를 생성하고 메일로 발송합니다.
     *
     * @param email 수신자 이메일 주소
     * @return 생성 및 발송된 6자리 숫자 인증 코드
     * @throws RuntimeException 메일 발송 중 기술적 오류가 발생할 경우 발생
     */
    String sendWithdrawalEmail(String email);
}