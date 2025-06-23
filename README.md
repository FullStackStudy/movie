# 🎬 Movie Booking Project

Spring Boot 기반 영화 예매 웹 애플리케이션입니다.  
사용자는 영화 목록 확인, 영화관 위치 보기, 예매, 결제 등을 할 수 있습니다.

---

## 📁 com.movie 패키지 구조

| 패키지명          | 설명 |
|-------------------|------|
| `config`          | 전역 설정 클래스 (Spring Security, WebMvc, CORS 등 설정) |
| `constant`        | 애플리케이션 전역에서 사용하는 상수 정의 (Enum, 코드값 등) |
| `controller`      | 웹 요청을 처리하는 컨트롤러 계층 (REST API 또는 MVC 패턴) |
| `dto`             | 계층 간 데이터 전달을 위한 객체 (요청/응답 DTO, 폼 데이터 등) |
| `entity`          | JPA 엔티티 클래스 (DB 테이블과 매핑되는 도메인 클래스) |
| `exception`       | 사용자 정의 예외 및 글로벌 예외 처리 클래스 (`@ControllerAdvice` 등) |
| `repository`      | JPA Repository 인터페이스 (데이터 접근 계층, DB 연동 처리) |
| `service`         | 핵심 비즈니스 로직을 처리하는 서비스 계층 (트랜잭션 포함) |
