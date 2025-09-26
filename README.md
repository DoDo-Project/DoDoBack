## ✨ 백엔드 주요 기능 (Key Backend Features)

- **✅ 사용자 및 펫 관리**
  > 회원 가입/로그인, 소셜 로그인(OAuth2) <br> 펫 등록, 정보 수정, 건강 기록 관리

- **✅ 산책 경로 및 실시간 위치 추적**
  > GPS 기반 실시간 위치 업데이트 (WebSocket) <br> 안전 구역(Geo-fencing) 설정 및 알림 (FCM)

- **✅ 활동 및 건강 통계**
  > 산책 기록, 활동량, 심박수, 수면 패턴 분석 <br> 통계 데이터 저장 및 조회 (MySQL + MyBatis)

- **✅ 커뮤니티**
  > 게시글 작성/조회/댓글 기능 <br> 검색 및 인기 게시글 추천

- **✅ 알림 및 푸시**
  > 활동 목표 달성 알림 <br> 지오펜싱 이탈 시 실시간 알림 <br> 커뮤니티 댓글/답글 알림

- **✅ 성능 최적화**
  > Redis 캐싱: 인기 산책 경로, 반려동물 위치 <br> RabbitMQ 기반 비동기 처리: 산책 데이터 후처리, 대량 알림

<br>

## ⚙️ 기술 스택 (Tech Stack)

<div align="center">

### Backend & Infrastructure
<p>
<img src="https://img.shields.io/badge/SpringBoot-6DB33F?style=for-the-badge&logo=spring&logoColor=white">
<img src="https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logo=amazon-aws&logoColor=white">
<img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white">
<img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white">
</p>

### Persistence & Real-time
<p>
<img src="https://img.shields.io/badge/JPA-2A3F54?style=for-the-badge&logoColor=white">
<img src="https://img.shields.io/badge/MyBatis-00599C?style=for-the-badge&logoColor=white">
<img src="https://img.shields.io/badge/WebSocket-0082C9?style=for-the-badge&logoColor=white">
<img src="https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white">
<img src="https://img.shields.io/badge/FCM-FCA121?style=for-the-badge&logo=firebase&logoColor=white">
</p>

</div>

<br>

## 🤖 백엔드 아키텍처 (Backend System Architecture)

백엔드 서버를 중심으로 사용자, 펫, 산책, 커뮤니티 데이터가 처리되는 구조 다이어그램  
(사용자 요청 → Spring Boot 서버 처리 → MySQL/JPA/MyBatis 데이터 저장 → Redis 캐싱 및 RabbitMQ 비동기 처리 → WebSocket/FCM을 통한 실시간 알림/위치 업데이트 → 앱/웹 클라이언트 제공)

<br>

## 🤝 Conventions
우리 프로젝트는 원활한 협업을 위해 아래와 같은 규칙을 따릅니다.

- **[Commit Convention](./.github/COMMIT_CONVENTION.md)**

<br>

## 📊 백엔드 참고자료 출처 (Sensor Source)

👉🏻 **[Spring Boot Microservices with RabbitMQ Example](https://rameshfadatare.medium.com/spring-boot-microservices-with-rabbitmq-example-92a38cbe08fc)**

<br>

## 💁‍♂️ 팀원 소개 (Team Members)

<table align="center">
  <tr>
    <td align="center">
      <a href="https://github.com/WhiteBin-bin">
      <img src="https://github.com/WhiteBin-bin.png" alt="백현빈 프로필" width="150" height="150"/><br>
      <b>백현빈</b>
    </td>
    <td align="center">
      <a href="https://github.com/limhb708">
      <img src="https://github.com/limhb708.png" alt="임현빈 프로필" width="150" height="150"/><br>
      <b>임현빈</b>
    </td>
  </tr>
</table>



