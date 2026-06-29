# AI Chatbot API

Kotlin/Spring Boot 기반 AI Chatbot API Project 입니다.

시연 가능한 핵심 흐름과 권한 경계를 먼저 잡는 것을 목표로 했습니다.

* 사용자 인증/인가
* 사용자/관리자 권한 분리
* 대화 생성 및 30분 기준 스레드 관리
* 피드백 생성/조회
* 관리자 통계 및 CSV 보고서
* 향후 AI Provider/RAG 확장을 고려한 경계 분리

---

## 1. 실행 방법

### Docker Compose

```bash
docker compose up --build
```

* API: `http://localhost:8080`
* Demo: `http://localhost:8080/`
* PostgreSQL: `localhost:5432`

### Local

로컬 PostgreSQL 실행 후 아래 명령어로 실행합니다.

```bash
export AUTH_JWT_SECRET="32자 이상 길이의 JWT secret"
export AI_API_KEY="OpenAI API Key"
./gradlew bootRun
```

---

## 2. 주요 API

대부분의 응답은 아래 형태를 사용합니다.

```json
{
  "success": true,
  "data": {}
}
```

에러는 `error.code`, `error.message`를 반환합니다.

### 인증 / 사용자

| Method | Path                | 인증  | 설명                             |
| ------ | ------------------- | --- | ------------------------------ |
| POST   | `/api/auth/signup`  | 불필요 | 회원가입                           |
| POST   | `/api/auth/login`   | 불필요 | access token, refresh token 발급 |
| POST   | `/api/auth/refresh` | 불필요 | access token 재발급               |
| POST   | `/api/auth/logout`  | 불필요 | refresh token revoke           |
| GET    | `/api/users/me`     | 필요  | 현재 사용자 조회                      |

### 대화 / 스레드

| Method | Path                                             | 인증 | 설명               |
| ------ | ------------------------------------------------ | -- | ---------------- |
| POST   | `/api/chats`                                     | 필요 | 질문 생성 및 AI 응답 저장 |
| GET    | `/api/chats?page=0&size=20&sort=lastChatAt,desc` | 필요 | 스레드 단위 대화 목록 조회  |
| DELETE | `/api/threads/{threadId}`                        | 필요 | 스레드 삭제           |

`POST /api/chats`

```json
{
  "question": "이 서비스는 어떤 기능을 제공하나요?",
  "isStreaming": false,
  "model": "gpt-5.5"
}
```

현재 `isStreaming=true`는 실제 스트리밍 대신 `STREAMING_NOT_READY` 에러를 반환합니다.

### 피드백

| Method | Path                                                    | 인증  | 설명        |
| ------ | ------------------------------------------------------- | --- | --------- |
| POST   | `/api/feedback`                                         | 필요  | 대화 피드백 생성 |
| GET    | `/api/feedback?positive=false&page=0&size=20&sort=desc` | 필요  | 피드백 목록 조회 |
| PATCH  | `/api/feedback/{feedbackId}/status`                     | 관리자 | 피드백 상태 변경 |

### 관리자

| Method | Path                            | 인증  | 설명                     |
| ------ | ------------------------------- | --- | ---------------------- |
| GET    | `/api/admin/analytics/activity` | 관리자 | 최근 24시간 가입/로그인/대화 생성 수 |
| GET    | `/api/admin/reports/chats.csv`  | 관리자 | 최근 24시간 대화 CSV 보고서     |

관리자 여부는 JWT의 `role=admin` claim으로 확인합니다.

---

## 3. 구현 범위

### 구현 완료

* 회원가입, 로그인, refresh token 재발급, logout
* 비밀번호 BCrypt 해시 저장
* refresh token 해시 저장 및 revoke
* JWT 기반 인증
* `member`, `admin` 역할 분리
* 현재 사용자 조회
* 대화 생성
* 30분 기준 스레드 재사용/신규 생성
* 같은 스레드의 이전 대화 목록을 AI 요청에 포함
* 스레드 목록 조회

  * 일반 사용자: 자신의 활성 스레드
  * 관리자: 전체 활성 스레드
* 스레드 soft delete
* 피드백 생성

  * 존재하는 `chatId` 검증
  * 일반 사용자는 자기 대화에만 피드백 가능
  * 동일 사용자/대화 중복 피드백 방지
* 피드백 목록 조회

  * 일반 사용자: 자신의 피드백
  * 관리자: 전체 피드백
* 관리자 피드백 상태 변경
* 최근 24시간 활동 통계
* 최근 24시간 대화 CSV 보고서
* 정적 데모 페이지
* Dockerfile / docker-compose.yml
* unit / slice / integration 테스트 태그 분리

### 부분 구현

* `isStreaming`

  * 요청 필드는 받지만 실제 SSE 스트리밍은 구현하지 않았습니다.
  * `true` 요청 시 `STREAMING_NOT_READY`를 반환합니다.
* `model`

  * 요청한 모델명을 OpenAI 요청에 전달합니다.
  * Provider 선택 기능은 없습니다.
* CSV 보고서

  * 최근 24시간 대화 기록을 최소 컬럼으로 반환합니다.
  * 기간 필터, 사용자 필터, 보고서 저장 이력은 없습니다.
* 관리자 계정

  * bootstrap 환경 변수가 있으면 시작 시 생성합니다.
  * 관리자 계정 관리 API는 없습니다.

---

## 4. 구현 전략

요구사항을 처음 봤을 때, 단순히 챗봇 API를 많이 구현하는 문제로 보지 않았습니다.

대화, 피드백, 관리자 보고 기능은 모두 현재 사용자와 권한에 따라 결과가 달라집니다. 그래서 먼저 인증/인가를 구현하고, 그 위에 각 도메인 기능을 올리는 순서로 진행했습니다.

구현 순서는 아래와 같습니다.

1. 사용자 인증/인가
2. 사용자/관리자 권한 경계
3. 대화 생성과 스레드 상태 관리
4. 피드백
5. 관리자 통계/보고
6. 데모 페이지

3시간 안에 모든 기능을 같은 깊이로 구현하기는 어렵다고 판단했습니다. 그래서 데모에서 실제로 확인할 수 있는 흐름과, 잘못 열리면 안 되는 권한 경계를 우선했습니다.

AI Provider도 같은 기준으로 분리했습니다. 현재는 OpenAI Responses API를 호출하지만, `ChatService`가 OpenAI HTTP 코드에 직접 묶이지 않도록 `AiClient` 인터페이스를 두었습니다.

RAG나 대외비 문서 학습은 현재 구현 범위가 아니라, 이후 `AiClient` 인터페이스 뒤에 붙일 수 있는 영역으로 남겨뒀습니다.

---

## 5. 가장 고민한 기능: 30분 기준 스레드 관리

가장 고민한 부분은 스레드 생성 자체가 아니라, 마지막 질문 시각을 어디에서 판단할지였습니다.

요구사항은 다음과 같습니다.

* 첫 질문이면 새 스레드 생성
* 마지막 질문 후 30분 이내이면 기존 스레드 유지
* 30분을 초과하면 새 스레드 생성
* AI 요청 시 같은 스레드의 이전 대화 목록 포함

처음에는 쿠키, JWT, DB 세 가지 방식을 비교했습니다.

### 쿠키 또는 클라이언트 상태

* 사용자가 값을 바꿀 수 있음
* 브라우저, 모바일, Postman에서 기준이 달라질 수 있음
* 여러 기기에서 같은 계정을 쓰면 스레드 기준이 흔들림

API 서버가 판단해야 할 대화 상태를 클라이언트에 맡기는 구조라고 봤습니다.

### JWT claim

JWT에 `lastQuestionAt`을 넣는 방식도 고려했습니다.

하지만 마지막 질문 시각은 질문할 때마다 바뀌는 값입니다.

* 매 질문마다 토큰 재발급 필요
* 이미 발급된 JWT의 값은 서버에서 즉시 변경 불가
* 여러 access token이 살아 있으면 상태가 갈라질 수 있음

그래서 JWT에는 사용자 id, email, role만 두었습니다. 대화 진행 상태는 JWT에 넣지 않았습니다.

### DB 기준

최종적으로 DB에 저장된 스레드/대화 상태를 기준으로 판단했습니다.

* 서버가 저장한 값이므로 신뢰 가능
* 여러 클라이언트에서 요청해도 동일 기준 사용
* 대화 목록, 스레드 삭제, 관리자 보고서와 같은 데이터 모델 공유 가능

JWT는 인증 상태를 표현하고, 대화 진행 상태는 DB에 남기는 쪽으로 구현했습니다.

---

## 6. AI 활용 내역

이번 과제에서는 OMX와 Codex를 사용했습니다.

AI를 사용한 부분과 직접 판단한 부분을 분리했습니다.

### AI를 사용한 부분

* Spring Boot 기본 구조 생성
* Controller / Service / Repository / DTO 반복 코드 작성
* Spring Security / JWT 초안 작성
* 테스트 태그 분리 구성 초안 작성
* 요구사항 체크리스트화
* 누락 API와 테스트 범위 점검
* README 초안 정리

### 직접 판단한 부분

* 인증/인가를 먼저 구현하기로 한 결정
* 일반 사용자와 관리자 권한 경계
* 사용자가 접근 가능한 데이터 범위
* 피드백 생성 시 대화 소유권 확인
* 스레드 30분 기준을 쿠키/JWT가 아니라 DB에 둔 결정
* 미구현 기능을 README에 명시한 결정

### 어려웠던 점

AI는 요구사항의 모든 기능을 비슷한 중요도로 다루는 경향이 있었습니다. 인증, 관리자 기능, 보고서까지 한 번에 크게 만들려는 Ai Slop과 같은 현상이 있었습니다.

하지만 구현량보다 제가 실제 데모를 제출해야하되는 사람처럼 클라이언트에게 보여줄 판단 과정이 더 중요하다고 생각했습니다. 그래서 스코프를 줄이고, 인증/인가와 스레드 상태 전이를 먼저 잡았습니다. 즉, AI는 구현을 담당하지만 구현 경계와 우선순위는 직접 정해야 했습니다.
