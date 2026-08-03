# 🚗 대물보상 MBTI — "너 T야?" (대물 사정사 한정)

자동차 **대물보상 직원**을 위한 MBTI 심리테스트입니다. 현장출동·견적협의·과실비율·미수선·렌트·구상 등 실제 대물 업무 상황 12문항에 답하면 16가지 유형 중 하나가 나옵니다. 직원 교육 아이스브레이킹용으로 만들었으며, **결과는 Neon(Postgres)에 자동 기록**되어 팀 전체 분포를 볼 수 있습니다. Vercel 호스팅 기준.

## 화면 구성

| 경로 | 설명 |
|------|------|
| `/` (`index.html`) | 이름 입력 → 12문항 → **결과 보기**(정산 로딩 연출) → 결과 + **공유하기** |
| `/records` (`records.html`) | 전체 기록: 참가자 목록 + 유형 분포 통계 |
| `/api/results` | 결과 저장(POST) / 조회(GET) 서버리스 함수 (Neon) |

## 🧭 4개 축 (표준 MBTI · 대물보상 버전)

| 축 | 앞글자 | 뒷글자 |
|----|--------|--------|
| **E / I** | 현장출동 E | 데스크분석 I |
| **S / N** | 규정·디테일 S | 큰그림·직관 N |
| **T / F** | 원칙·칼정산 T | 공감·고객 F |
| **J / P** | 계획·칼마감 J | 순발력·유연 P |

각 축 3문항(홀수)이라 동점 없이 항상 한 유형으로 결정됩니다.

## 🗂️ 결과에 담기는 것 (개량판)
- 유형 이모지·이름·**자주 하는 말(캐치프레이즈)**
- 성격 설명 + **💪 강점 / 😅 주의**
- **🎯 이런 사건에 강해요 / 🥵 이런 사건엔 진땀** (대물 사건유형 기반)
- 💬 디비의 한마디 · 🧭 성향 좌표(4축 비율) · 🤝 동료 궁합(짝꿍/상극)

> 16유형은 4개 기질군(분석형/외교형/관리형/탐험형)으로 색이 구분됩니다.

---

## 🚀 Vercel 배포

1. **저장소 연결**: Vercel → Add New… → Project → 이 GitHub 저장소 Import (프리셋 **Other**).
2. **Neon 연결**: 프로젝트 → **Storage → Create/Connect Database → Neon**. 연결하면 `DATABASE_URL`이 자동 주입됩니다. (이미 Neon이 있으면 Settings → Environment Variables에 `DATABASE_URL` 직접 입력. 인식 순서: `DATABASE_URL` → `POSTGRES_URL`)
3. **배포**: 브랜치에 push 하면 자동 빌드/배포. 테이블(`results`)은 **첫 요청 시 자동 생성**됩니다.

### 로컬 실행
```bash
npm install
cp .env.example .env      # .env 에 Neon DATABASE_URL 입력
npx vercel dev            # http://localhost:3000
```
> 기록 저장/조회는 `/api`가 도는 `vercel dev` 또는 실제 배포에서만 동작합니다. 정적으로 `index.html`만 열면 저장은 안 되지만 결과 확인은 됩니다.

## 🗄️ 데이터 (`results` 테이블)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | bigserial | PK |
| `name` | text | 참가자 이름/닉네임 |
| `code` | text | 유형 코드 (예: `ESTJ`) |
| `type_name` | text | 유형 이름 (예: 대물 지휘관) |
| `answers` | text | 12문항 a/b 응답 |
| `created_at` | timestamptz | 기록 시각 |

- `POST /api/results` — `{ name, code, typeName, answers }` → `{ ok, id }`
- `GET /api/results?limit=1000` — `{ ok, total, results, counts }`

## 🛠️ 커스터마이징
콘텐츠는 전부 `index.html` 안에 있습니다.
- **질문**: `QUESTIONS` (선택지 `a`=앞글자, `b`=뒷글자, `d`=축)
- **유형**: `TYPES` (코드별 `name`/`say`/`desc`/`strong`/`watch`/`strongCase`/`weakCase`/`advice`/`good`/`bad`)
- **기질군 색상**: `GROUPS`

> 재미로 보는 테스트이며, 실제 심리검사(MBTI®)와는 무관합니다.
