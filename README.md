# 🎁 보상 MBTI — 직원 교육 아이스브레이킹용 (Vercel + Neon)

직원 교육의 아이스브레이킹 시간에 쓰는 **"나를 춤추게 하는 보상 유형" 성향 테스트**입니다.
참가자가 이름을 입력하고 12문항에 답하면 16가지 보상 성향 중 하나가 나오며, **결과는 Neon(Postgres)에 자동 기록**되어 전체 기록 페이지에서 모아 볼 수 있습니다.

## 화면 구성

| 경로 | 설명 |
|------|------|
| `/` (`index.html`) | 랜딩(이름 입력 + 시작하기) → 퀴즈 → **결과 보기** → 결과 + **공유하기** |
| `/records` (`records.html`) | 전체 기록: 참가자 목록 + 유형 분포 통계 |
| `/api/results` | 결과 저장(POST) / 조회(GET) 서버리스 함수 |

## 🧭 보상 성향 4개 축 (2×2×2×2 = 16유형)

| 축 | 한쪽 | 다른 쪽 |
|----|------|---------|
| **형태** | 실속형(M) · 돈/물질 | 가치형(E) · 경험/의미 |
| **타이밍** | 즉시형(I) · 바로바로 | 축적형(A) · 모아서 크게 |
| **대상** | 개인형(S) · 나부터 | 팀형(T) · 함께 |
| **방식** | 스포트라이트형(P) · 공개 | 은근형(Q) · 조용히 |

각 축은 3문항(홀수)이라 동점 없이 항상 한 유형으로 결정됩니다.

---

## 🚀 Vercel 배포

### 1) 저장소 연결
Vercel 대시보드 → **Add New… → Project** → 이 GitHub 저장소 선택 → Import.
프레임워크 프리셋은 **Other**로 두면 됩니다 (정적 파일 + `/api` 서버리스 함수 자동 인식).

### 2) Neon 데이터베이스 연결
Vercel 프로젝트 → **Storage → Create/Connect Database → Neon** 선택.
연결하면 `DATABASE_URL` 환경변수가 프로젝트에 자동으로 주입됩니다.

> 이미 Neon 프로젝트가 있다면, Vercel **Settings → Environment Variables** 에
> `DATABASE_URL` 값을 직접 넣어도 됩니다. (`DATABASE_URL` → `POSTGRES_URL` 순으로 인식)

### 3) 배포
`main` 브랜치(또는 이 브랜치)로 push 하면 Vercel이 자동 빌드/배포합니다.
배포 후 접속하면 끝 — **테이블(`results`)은 첫 요청 시 자동 생성**되므로 별도 마이그레이션이 필요 없습니다.

### 로컬 실행 (선택)
```bash
npm install
cp .env.example .env      # .env 에 Neon DATABASE_URL 입력
npx vercel dev            # http://localhost:3000
```
> 정적 파일만 미리 보려면 아무 정적 서버로 `index.html`을 열어도 되지만,
> 기록 저장/조회는 `/api`가 동작하는 `vercel dev` 또는 실제 배포 환경에서만 됩니다.

---

## 🗄️ 데이터 (`results` 테이블)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | bigserial | PK |
| `name` | text | 참가자 이름/닉네임 |
| `code` | text | 유형 코드 (예: `MISP`) |
| `type_name` | text | 유형 이름 (예: 무대 위 인센티브 헌터) |
| `answers` | text | 12문항 응답 축 글자 (예: `MEMIAISTPQP`) |
| `created_at` | timestamptz | 기록 시각 |

### API
- `POST /api/results` — body `{ name, code, typeName, answers }` → `{ ok, id }`
- `GET /api/results?limit=1000` — `{ ok, total, results:[…], counts:[{code,count}] }`

---

## 🧑‍🏫 진행자 팁 (아이스브레이킹 흐름 예시)

1. **개별 진행**: 각자 이름 입력 후 테스트 (2~3분).
2. **자기소개 라운드**: "저는 OO형이고, 이런 보상에 심장이 뛴대요!" 한 줄씩.
3. **전체 기록 보기**: `/records`를 스크린에 띄워 우리 조직에 어떤 유형이 많은지 함께 확인.
4. **리더 교육 연계**: 결과의 *매니저 팁*으로 "우리 팀원을 어떻게 동기부여할까?" 토론.

> 재미와 상호 이해를 위한 활동이며, 실제 심리검사(MBTI®)와는 무관합니다.

## 🛠️ 커스터마이징
모든 콘텐츠는 `index.html` 안에 있습니다.
- **질문**: `QUESTIONS` 배열 (각 선택지 `k` 값이 축 글자 M/E/I/A/S/T/P/Q)
- **유형 설명**: `TYPES` 객체 (코드별 `n`/`tag`/`desc`/`loves`/`tip`/`good`/`bad`)
- **색상**: 상단 `<style>`의 `:root` 변수
