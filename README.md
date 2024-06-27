## 항해 첫주차 과제 TDD
### 과제 필수사항
- Nest.js 의 경우 Typescript , Spring 의 경우 Kotlin / Java 중 하나로 작성합니다.
- 프로젝트에 첨부된 설정 파일은 수정하지 않도록 합니다.
- 테스트 케이스의 작성 및 작성 이유를 주석으로 작성하도록 합니다.
- 프로젝트 내의 주석을 참고하여 필요한 기능을 작성해주세요.
- 분산 환경은 고려하지 않습니다.

### 요구사항
- PATCH /point/{id}/charge : 포인트를 충전한다.
- PATCH /point/{id}/use : 포인트를 사용한다.
- GET /point/{id} : 포인트를 조회한다.
- GET /point/{id}/histories : 포인트 내역을 조회한다.
- 잔고가 부족할 경우, 포인트 사용은 실패하여야 합니다.
- 동시에 여러 건의 포인트 충전, 이용 요청이 들어올 경우 순차적으로 처리되어야 합니다. (동시성)

### 요구사항 분석
- 포인트를 충전한다
- 0원의 충전 요구가 들어왔을 때 Exception
- 포인트 내역이 정상적으로 등록되지 않으면 Exception
- 포인트를 사용한다
- 잔고가 부족할 경우 Exception
- 0원 사용을 요청할 경우 Exception
- 포인트 내역이 정상적으로 등록되지 않으면 Exception
- 포인트를 조회한다
- 포인트라는 Entity가 UserId로 생성되어있지 않으면 Exception이나 현재 Database 구현체에서 Default값을 보내주니 Skip
- 포인트 내역을 조회한다.

### 동시성 테스트
- synchronized 사용
- 순차적인 처리 요구조건 충족 x
- ThreadPoolExecutor 사용
- 순차적인 처리 요구조건 충족 x
- Test코드 Unit Test 어려움
