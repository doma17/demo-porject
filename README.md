# AI Chatbot API

Kotlin/Spring Boot로 구현한 AI Chatbot 과제 API입니다. 3시간 제한 안에서 **실행 가능한 데모**를 먼저 만들고, 인증/인가와 스레드 상태 전이를 중심으로 구현 범위를 정했습니다.

이 README는 먼저 실행 방법과 API 사용법을 설명하고, 뒤쪽에 과제 분석, AI 활용 내역, 제한 사항을 정리합니다.

---

## 1. 빠른 실행

### 요구 환경

- Java 17
- Docker / Docker Compose
- Gradle Wrapper 포함
- PostgreSQL은 Docker Compose로 실행 가능

### 필수 환경 변수

`.env` 파일 또는 shell 환경 변수로 설정합니다. 실제 API Key 값은 문서에 적지 않습니다.

```bash
AUTH_JWT_SECRET=32자_이상의_JWT_SECRET
AI_API_KEY=OpenAI_API_Key
```

선택 값:

```bash
POSTGRES_DB=demo_project
POSTGRES_USER=demo
POSTGRES_PASSWORD=demo
POSTGRES_PORT=5432
APP_PORT=8080

AI_MODEL=gpt-5.5
AI_BASE_URL=https://api.openai.com/v1
AI_TIMEOUT=30s

# 선택: 관리자 계정 자동 생성
ADMIN_BOOTSTRAP_EMAIL=admin@example.com
ADMIN_BOOTSTRAP_PASSWORD=password123
ADMIN_BOOTSTRAP_NAME=Admin
```

`AUTH_JWT_SECRET`은 코드에서 32자 이상으로 검증합니다.

### Docker Compose 실행

```bash
docker compose up --build
```

접속:

- API: `http://localhost:8080`
- 데모 페이지: `http://localhost:8080/`
- PostgreSQL: `localhost:5432`

### 로컬 실행

로컬 PostgreSQL이 떠 있어야 합니다. 기본 DB 설정은 `application.yml` 기준입니다.

```bash
export AUTH_JWT_SECRET="32자 이상 길이의 JWT secret"
export AI_API_KEY="OpenAI API Key"
./gradlew bootRun
```

---

## 2. 테스트

테스트는 하나의 `src/test` source set 안에서 JUnit Tag로 분리했습니다.

```bash
# 전체 테스트
./gradlew test

# 단위 테스트
./gradlew unitTest

# Controller/Spring slice 테스트
./gradlew sliceTest

# Testcontainers 기반 통합 테스트
./gradlew integrationTest
```

통합 테스트는 PostgreSQL Testcontainers를 사용하므로 Docker가 필요합니다.

---

## 3. 주요 API

대부분의 JSON API는 아래 응답 형태를 사용합니다.

```json
{
  "success": true,
  "data": {}
}
```

에러는 `error.code`, `error.message`를 반환합니다.

### 인증 / 사용자

| Method | Path | 인증 | 설명 |
| --- | --- | --- | --- |
| POST | `/api/auth/signup` | 불필요 | 회원가입 |
| POST | `/api/auth/login` | 불필요 | access token, refresh token 발급 |
| POST | `/api/auth/refresh` | 불필요 | refresh token으로 access token 재발급 |
| POST | `/api/auth/logout` | 불필요 | refresh token revoke |
| GET | `/api/users/me` | 필요 | 현재 사용자 조회 |

회원가입:

```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "User"
}
```

로그인 후 받은 access token을 사용합니다.

```http
Authorization: Bearer {accessToken}
```

### 대화 / 스레드

| Method | Path | 인증 | 설명 |
| --- | --- | --- | --- |
| POST | `/api/chats` | 필요 | 질문 생성 및 AI 응답 저장 |
| GET | `/api/chats?page=0&size=20&sort=lastChatAt,desc` | 필요 | 스레드 목록 조회 |
| DELETE | `/api/threads/{threadId}` | 필요 | 스레드 삭제 처리 |

대화 생성:

```json
{
  "question": "이 서비스는 어떤 기능을 제공하나요?",
  "isStreaming": false,
  "model": "gpt-5.5"
}
```

현재 `isStreaming=true`는 실제 스트리밍이 아니라 `501 NOT_IMPLEMENTED`를 반환합니다.

### 피드백

| Method | Path | 인증 | 설명 |
| --- | --- | --- | --- |
| POST | `/api/feedback` | 필요 | 대화 피드백 생성 |
| GET | `/api/feedback?positive=false&page=0&size=20&sort=desc` | 필요 | 피드백 목록 조회 |
| PATCH | `/api/feedback/{feedbackId}/status` | 관리자 | 피드백 상태 변경 |

피드백 생성:

```json
{
  "chatId": "00000000-0000-0000-0000-000000000000",
  "positive": false
}
```

피드백 상태 변경:

```json
{
  "status": "resolved"
}
```

### 관리자 분석 / 보고

| Method | Path | 인증 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/admin/analytics/activity` | 관리자 | 최근 24시간 가입/로그인/대화 생성 수 |
| GET | `/api/admin/reports/chats.csv` | 관리자 | 최근 24시간 대화 CSV 보고서 |

관리자 여부는 JWT의 `role` claim이 `admin`인지 확인합니다.

---

## 4. 구현 범위

### 구현 완료

- 회원가입, 로그인, refresh token 재발급, logout
- 비밀번호 BCrypt 해시 저장
- refresh token 해시 저장 및 revoke
- JWT 기반 인증
- `member`, `admin` 역할 구분
- 현재 사용자 조회
- 대화 생성
- 30분 기준 스레드 재사용/신규 생성
- 같은 스레드의 이전 대화 목록을 AI 요청에 포함
- 스레드 목록 조회
  - 일반 사용자: 자신의 활성 스레드
  - 관리자: 전체 활성 스레드
- 스레드 soft delete
- 피드백 생성
  - 존재하는 `chatId` 검증
  - 일반 사용자는 자기 대화에만 피드백 가능
  - 동일 사용자/대화 중복 피드백 방지
- 피드백 목록 조회
  - 일반 사용자: 자신의 피드백
  - 관리자: 전체 피드백
- 관리자 피드백 상태 변경
- 최근 24시간 활동 통계
- 최근 24시간 대화 CSV 보고서
- 정적 데모 페이지 `src/main/resources/static/index.html`
- Dockerfile / docker-compose.yml
- unit / slice / integration 테스트 태그 분리

### 부분 구현

- `isStreaming`
  - 요청 필드는 있지만 실제 SSE 스트리밍은 없습니다.
  - `true`이면 `STREAMING_NOT_READY` 에러를 반환합니다.
- `model`
  - 요청한 모델명을 OpenAI 요청에 넘깁니다.
  - Provider를 사용자가 선택하는 기능은 없습니다.
- CSV 보고서
  - 최근 24시간 대화 기록을 최소 컬럼으로 반환합니다.
  - 기간 필터, 사용자 필터, 저장 이력은 없습니다.
- 관리자 계정
  - bootstrap 환경 변수가 있으면 시작 시 생성됩니다.
  - 관리자 계정 관리 API는 없습니다.

### 구현하지 않은 것

- 실제 SSE Streaming
- RAG 기반 문서 검색
- 대외비 문서 업로드/파싱/임베딩
- Vector DB
- OpenAI retry/rate limit/circuit breaker
- Swagger/OpenAPI
- Flyway/Liquibase migration
- 동시 요청 시 active thread 중복 생성 방어

---

## 5. 과제 분석과 구현 순서

처음 요구사항을 봤을 때 단순히 “챗봇 API를 많이 만드는 문제”로 보지 않았습니다. 대화, 피드백, 관리자 보고 기능은 모두 현재 사용자와 권한에 따라 결과가 달라집니다.

그래서 구현 순서를 이렇게 잡았습니다.

1. 사용자 인증/인가
2. 대화 생성과 스레드 상태 관리
3. 피드백
4. 관리자 통계/보고
5. 데모 페이지

인증/인가를 먼저 만든 이유는 분명했습니다. 사용자 경계가 없으면 피드백 소유권도, 관리자 보고 권한도, 대화 목록 범위도 제대로 정할 수 없기 때문입니다.

3시간 안에 모든 기능을 같은 깊이로 구현하기는 어렵다고 판단했습니다. 그래서 “데모에서 실제로 눌러볼 수 있는 핵심 흐름”과 “권한을 잘못 열지 않는 것”을 먼저 봤습니다.

AI Provider도 같은 기준으로 판단했습니다. 현재는 OpenAI Responses API를 호출하지만, `ChatService`가 OpenAI HTTP 코드에 직접 묶이지 않도록 `AiClient` 인터페이스를 두었습니다. RAG나 문서 학습은 지금 구현 범위가 아니라, 나중에 이 경계 뒤에 붙일 수 있는 영역으로 남겼습니다.

---

## 6. 가장 고민했던 기능: 30분 스레드 판단

가장 고민했던 기능은 스레드 생성 자체가 아니었습니다. **마지막 질문 후 30분 이내이면 기존 스레드를 유지한다**는 요구사항에서, 마지막 질문 시각을 어디에서 믿을 것인지가 핵심이었습니다.

### 선택지 1. 쿠키 또는 클라이언트 상태

구현은 쉽습니다. 하지만 서버가 믿기 어렵습니다.

- 사용자가 값을 바꿀 수 있습니다.
- 브라우저, 모바일, Postman에서 기준이 달라질 수 있습니다.
- 여러 기기에서 같은 계정을 쓰면 스레드 기준이 흔들립니다.

그래서 선택하지 않았습니다.

### 선택지 2. JWT claim

JWT는 사용자를 식별하는 데는 좋습니다. 하지만 마지막 질문 시각은 질문할 때마다 바뀌는 값입니다.

- 매 질문마다 토큰을 다시 발급해야 합니다.
- 이미 발급된 JWT 안의 값은 서버가 즉시 바꿀 수 없습니다.
- 여러 access token이 살아 있으면 서로 다른 상태를 가질 수 있습니다.

그래서 JWT에는 사용자 id, email, role만 두었습니다. 대화 진행 상태는 넣지 않았습니다.

### 선택지 3. DB의 스레드/대화 상태

최종적으로 DB 기준을 선택했습니다.

- 서버가 저장한 값이라 조작하기 어렵습니다.
- 여러 클라이언트에서 요청해도 같은 기준을 씁니다.
- 대화 목록, 스레드 삭제, 관리자 보고서와 같은 데이터 모델을 공유할 수 있습니다.

현재 상태 전이는 다음과 같습니다.

1. 현재 사용자의 삭제되지 않은 최신 스레드를 `lastChatAt desc`로 조회합니다.
2. 최신 스레드가 없으면 새 스레드를 만듭니다.
3. 최신 스레드의 `lastChatAt`이 현재 시각 기준 30분 이내이면 기존 스레드를 씁니다.
4. 정확히 30분 전이면 기존 스레드를 씁니다.
5. 30분을 초과하면 새 스레드를 만듭니다.
6. 삭제된 스레드는 재사용하지 않습니다.
7. 같은 스레드의 이전 대화를 시간순으로 읽어 AI 요청에 함께 보냅니다.

이 기능은 단순 CRUD보다 상태 기준을 어디에 둘지 정하는 문제가 더 중요했습니다. 마지막 질문 시각은 사용자가 보내주는 값이 아니라 서버가 저장한 대화 데이터를 기준으로 판단하는 것이 맞다고 봤습니다.

JWT는 인증 상태를 표현하는 데 사용하고, 대화의 진행 상태는 DB에 남기는 쪽으로 경계를 나눴습니다. 이 결정 덕분에 여러 클라이언트에서 요청해도 같은 기준으로 스레드를 결정할 수 있습니다.

다만 동시에 같은 사용자가 여러 요청을 보내는 race condition은 3시간 제한상 완전히 다루지 못했습니다. 운영 환경에서는 사용자별 active thread 관리, transaction isolation, lock, unique constraint 등을 추가로 검토해야 합니다.

---

## 7. AI 활용 내역

이번 과제에서는 OMX와 Codex를 사용했습니다. AI를 숨기기보다, 어떤 일을 맡기고 어떤 판단은 직접 했는지 나누어 정리합니다.

### AI를 사용한 부분

- Spring Boot 기본 구조 생성
- Controller / Service / Repository / DTO 반복 코드 작성
- Spring Security / JWT 초안 작성
- 테스트 태그 분리 구성 초안 작성
- 요구사항 체크리스트화
- 누락된 API와 테스트 범위 점검
- README 초안 정리

### 직접 판단한 부분

- 인증/인가를 가장 먼저 구현하기로 한 결정
- 일반 사용자와 관리자 권한 경계
- 사용자가 접근 가능한 데이터 범위
- 피드백 생성 시 대화 소유권을 확인해야 한다는 결정
- 스레드 30분 기준을 쿠키/JWT가 아니라 DB에 둔 결정
- AI Provider 경계는 두되 RAG/문서학습은 3시간 범위 밖으로 둔 결정
- 미구현 기능을 숨기지 않고 README에 남기는 결정

### AI 사용 중 어려웠던 점

AI는 기능을 모두 같은 중요도로 다루는 경향이 있었습니다. 인증, RAG, streaming, 관리자 기능, 보고서까지 한 번에 크게 만들려는 제안도 있었습니다.

하지만 이 과제는 구현량보다 판단 과정이 더 중요하다고 봤습니다. 그래서 범위를 줄였습니다. 먼저 인증/인가와 스레드 상태 전이를 세웠고, 나머지는 데모 가능한 최소 흐름으로 붙였습니다.

개인적으로는 이 부분이 가장 중요했습니다. 짧은 시간 안에 완성도를 보여주려면 많이 만드는 것보다, 무엇을 먼저 지켜야 하는지 정해야 했습니다. 이 프로젝트에서는 그 기준이 사용자 경계와 대화 상태였습니다.

---

## 8. 제한 사항과 향후 확장

아래 항목은 “못 숨긴 미완성”이 아니라, 3시간 안에 핵심 흐름을 먼저 만들기 위해 의도적으로 뒤로 둔 부분입니다.

- SSE Streaming
  - 현재는 요청을 받되 명시적으로 미구현 에러를 반환합니다.
- RAG / 문서 학습
  - 현재는 대화 history만 AI에 전달합니다.
  - 문서 업로드, chunking, embedding, Vector DB 검색은 없습니다.
- AI 장애 대응
  - OpenAI 호출 실패는 `AI_PROVIDER_ERROR`로 변환합니다.
  - retry, rate limit, fallback Provider는 없습니다.
- 동시성 제어
  - 같은 사용자가 동시에 질문할 때 스레드가 중복 생성될 가능성을 완전히 막지 않았습니다.
- 관리자 기능
  - 관리자 bootstrap은 가능하지만 계정 관리 API는 없습니다.
- 보고서
  - 최근 24시간 CSV만 제공합니다.
  - 기간/사용자 필터는 없습니다.
- API 문서
  - Swagger는 없습니다.
  - README와 `index.html` 데모 페이지로 확인합니다.

---

## 9. 짧은 회고

처음에는 챗봇 기능을 더 많이 붙이고 싶은 마음이 있었습니다. 하지만 제한 시간이 있는 과제에서는 많이 붙인 기능보다, 실제로 실행되고 설명 가능한 흐름이 더 중요하다고 판단했습니다.

그래서 사용자 인증, 권한 경계, 30분 스레드 상태를 먼저 잡았습니다. 이 세 가지가 잡히면 피드백과 보고 기능도 같은 기준 위에 올릴 수 있다고 봤습니다.

완성하지 못한 부분도 있습니다. 특히 streaming, RAG, 동시성 제어는 더 다듬고 싶었습니다. 그래도 이번 구현에서는 “어디까지 만들었고, 무엇을 일부러 남겼는지”를 숨기지 않는 것이 더 낫다고 생각했습니다.
