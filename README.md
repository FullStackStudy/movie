# 🎬 MovieFlex Project

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

## 주요 기능

- 영화 정보 조회 및 예매
- 회원가입 및 로그인
- 이메일 인증 기능
- 마이페이지 관리
- OAuth2 소셜 로그인 (Google)
- 외부 사이트를 selenium 과 jsoup 을 이용하여 크롤링
- 메인배너에 영화정보 팝업
- quartz 스케쥴러를 활용하여 매시간 토큰삭제 및 재차 크롤링
- movieList 페이지 infinite scroll 기능 적용
- 데이터 캐싱
- 리뷰 게시판
- 결제화면 제작 (/payment/test)
- 카카오페이/신용카드 결제 구현 (임시카드번호 4532015112830366 만료일 12/25 CVC 123 소유자명 홍길동 )

### test 계정
- test@test.com / 1234
- admin@admin.com / 1234

### email 인증 기능 설명
- 회원가입 시 이메일 인증을 통한 본인 확인
- 6자리 숫자 인증 코드 전송
- 5분간 유효한 인증 코드
- 만료된 토큰 자동 정리

### 설정 방법

1. **Gmail 앱 비밀번호 설정**
   - Gmail 계정에서 2단계 인증 활성화
   - 앱 비밀번호 생성
   - `application.properties`에서 이메일 설정 수정

```properties
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

2. **API 엔드포인트**
   - `POST /api/email/send-code`: 인증 코드 전송
   - `POST /api/email/verify-code`: 인증 코드 확인
   - `POST /api/email/check-duplicate`: 이메일 중복 확인

### 사용 방법

1. 회원가입 페이지에서 이메일 입력
2. "인증코드 전송" 버튼 클릭
3. 이메일로 받은 6자리 코드 입력
4. "인증 확인" 버튼 클릭
5. 인증 완료 후 회원가입 진행

## 기술 스택

- **Backend**: Spring Boot 3.5.2, Spring Security, Spring Data JPA, Selenium
- **Database**: MySQL, H2
- **Frontend**: Thymeleaf, Bootstrap, JavaScript
- **Email**: Spring Mail (Gmail SMTP)
- **Scheduler**: Quartz
- **Documentation**: Swagger(API자동문서화)/OpenAPI

## 실행 방법

1. **환경 설정**
   ```bash
   # MySQL 데이터베이스 생성
   CREATE DATABASE movie;
   
   # application.properties에서 데이터베이스 설정 확인
   ```

2. **애플리케이션 실행**
   ```bash
   ./mvnw spring-boot:run
   ```

3. **접속**
   - 메인 페이지: http://localhost:80


