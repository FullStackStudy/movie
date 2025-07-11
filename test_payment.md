# 결제 기능 테스트 가이드

## 테스트 계정 정보
- **회원 ID**: test@test.com
- **비밀번호**: 1234
- **초기 포인트**: 12000P

## 테스트 시나리오

### 1. 테스트 결제 페이지 접근
```
GET http://localhost:/payment/test
```

**예상 결과:**
- 결제 페이지가 정상적으로 로드됨
- 회원 ID: test@test.com
- 보유 포인트: 12000P
- 영화: 범죄도시4
- 가격: 12000원

### 2. 포인트 결제 테스트
1. 포인트 사용량을 5000P로 설정
2. "포인트 결제" 버튼 클릭
3. 결제 완료 후 포인트 차감 확인

**예상 결과:**
- 결제 성공 페이지 표시
- 포인트 차감: 12000P → 7000P
- 결제 내역에 기록 추가

### 3. 카드결제 테스트
1. 포인트 사용량을 3000P로 설정
2. "신용카드로 결제" 버튼 클릭
3. 카드 정보 입력:
   - 카드번호: 4532015112830366
   - 만료일: 12/25
   - CVC: 123
   - 소유자명: 테스트유저

**예상 결과:**
- 결제 성공 페이지 표시
- 포인트 차감: 12000P → 9000P
- 결제 금액: 9000원 (12000원 - 3000P)
- 거래번호 생성

### 4. 카카오페이 결제 테스트
1. 포인트 사용량을 2000P로 설정
2. "카카오페이로 결제" 버튼 클릭
3. 카카오페이 결제 페이지로 리다이렉트

**예상 결과:**
- 카카오페이 결제 페이지로 이동
- 결제 금액: 10000원 (12000원 - 2000P)

## 검증 포인트

### 1. 데이터베이스 검증
```sql
-- 회원 정보 확인
SELECT member_id, point, reserve FROM member WHERE member_id = 'test@test.com';

-- 결제 후 포인트 차감 확인
-- 결제 내역(reserve) 필드에 새로운 기록 추가 확인
```

### 2. 로그 확인
- 애플리케이션 로그에서 결제 처리 과정 확인
- 에러 발생 시 상세 로그 확인

### 3. UI 검증
- 결제 페이지에서 포인트 표시 정확성
- 결제 방법 선택 버튼 동작
- 결제 성공/실패 페이지 표시

## 문제 해결

### 1. 테스트 계정이 없는 경우
- 애플리케이션 재시작 시 `TestAccountInitializer`가 자동으로 계정 생성
- 또는 수동으로 계정 생성

### 2. 포인트 표시 오류
- `member.getPoint()` 값 확인
- 숫자 파싱 로직 검증

### 3. 결제 실패
- 로그에서 에러 메시지 확인
- 카드 정보 유효성 검사 확인
- 카카오페이 설정 확인

## 테스트 완료 후

### 1. 포인트 복원
```sql
UPDATE member SET point = '12000' WHERE member_id = 'test@test.com';
```

### 2. 결제 내역 초기화
```sql
UPDATE member SET reserve = '' WHERE member_id = 'test@test.com';
```

## 예상 로그 메시지
```
✅ 테스트용 USER 계정 생성 완료: test@test.com / 1234
테스트 결제 페이지 - 회원: test@test.com, 포인트: 12000
카드결제 처리 중: 주문번호=ORDER_xxx, 금액=9000
카드결제 성공: 거래번호=TXN_XXXX
``` 