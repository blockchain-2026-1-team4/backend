# TrustTicket 시연 시나리오 A/B 실행 가이드

이 문서는 TrustTicket 데모를 처음 실행하는 팀원도 동일한 순서로 따라 할 수 있도록 정리한 실행 지침입니다.
특정 개인 PC 경로에 의존하지 않으며, 아래의 `<프로젝트_ROOT>`는 `blockchain-backend`, `blockchain-frontend`, `blockchain-contract` 폴더가 들어 있는 상위 폴더를 의미합니다.

## 1. 데모 목표

### 시나리오 A

NFT 티켓이 발행되고, 사용자 A가 1차 구매한 뒤 공식 리셀로 사용자 B에게 판매되며, 이후 QR 체크인까지 이어지는 메인 흐름을 검증합니다.

핵심 검증 포인트:

- 티켓은 DB 데이터만이 아니라 TrustTicket 컨트랙트의 ERC721 NFT입니다.
- 1차 구매 후 `ownerOf(tokenId)`가 TrustTicket 컨트랙트에서 사용자 A 지갑으로 바뀝니다.
- 리셀 등록과 리셀 구매는 컨트랙트 정책을 통과해야 합니다.
- 리셀 구매 후 `ownerOf(tokenId)`가 사용자 B 지갑으로 바뀝니다.
- 체크인 후 티켓은 사용 처리되어 재사용할 수 없습니다.

### 시나리오 B

외부 팬클럽이 발급한 FanClubMembership NFT를 티켓 플랫폼이 온체인에서 검증하여, 멤버십 보유자만 선예매 기간에 구매할 수 있음을 검증합니다.

핵심 검증 포인트:

- 사용자 A는 FanClubMembership NFT를 보유합니다.
- 사용자 B는 FanClubMembership NFT를 보유하지 않습니다.
- 선예매 정책이 적용된 이벤트에서 사용자 B 구매는 실패합니다.
- 같은 이벤트에서 사용자 A 구매는 성공합니다.

## 2. 테스트넷 정보

```text
Network: Kaia Kairos Testnet
Chain ID: 1001
RPC: https://public-en-kairos.node.kaia.io

TrustTicket:
0x790aa2356BAb711998faA9c58dCDD47205e6683d

FanClubMembership:
0xCA64026A80a9295aE1829DeDcb143dB23C3A3300
```

## 3. 사전 준비

### 3.1 필요한 프로그램

- Docker
- Java 21 이상
- Node.js
- npm
- Chrome 브라우저
- Chrome에 설치된 MetaMask
- Foundry `cast` 명령어

Foundry가 없다면 설치 후 터미널을 다시 열어주세요.

```bash
curl -L https://foundry.paradigm.xyz | bash
foundryup
```

### 3.2 MetaMask 네트워크

MetaMask에 Kaia Kairos Testnet을 추가합니다.

```text
Network name: Kaia Kairos Testnet
RPC URL: https://public-en-kairos.node.kaia.io
Chain ID: 1001
Currency symbol: KAIA
Block explorer: https://kairos.kaiascan.io
```

### 3.3 테스트 지갑

시연에는 최소 3개의 지갑이 있으면 좋습니다.

```text
운영자/주최자 지갑: 이벤트 생성, 티켓 발행, 멤버십 선예매 설정
사용자 A 지갑: 1차 구매자, 리셀 판매자, 멤버십 NFT 보유자
사용자 B 지갑: 리셀 구매자, 멤버십 NFT 미보유자
```

각 지갑에 Kairos KAIA를 충전합니다.

```text
https://faucet.kaia.io
```

### 3.4 컨트랙트 권한

운영자/주최자 지갑에는 다음 권한이 필요합니다.

```text
TrustTicket: ORGANIZER_ROLE
FanClubMembership: MEMBERSHIP_ISSUER_ROLE
```

관리자까지 직접 테스트하려면 `DEFAULT_ADMIN_ROLE`도 있으면 편합니다. 단, `DEFAULT_ADMIN_ROLE`이 있다고 해서 자동으로 모든 역할을 가진다는 뜻은 아닙니다. 관리자 계정은 역할을 부여할 수 있고, 실제 함수 호출에는 함수가 요구하는 역할이 필요합니다.

### 3.5 개인키 설정

백엔드가 이벤트 생성과 티켓 민팅을 운영자 지갑으로 자동 서명하려면 운영자 지갑 개인키가 필요합니다.

개인키는 절대 채팅, 문서, Git에 저장하지 마세요. 실행 터미널에서만 환경변수로 설정합니다.

```bash
export OPERATOR_PRIVATE_KEY='0x를_제외한_64자리_개인키'
export OPERATOR_PRIVATE_KEY=${OPERATOR_PRIVATE_KEY#0x}
```

정상 형식인지 확인합니다. 이 명령은 개인키를 출력하지 않습니다.

```bash
python3 - <<'PY'
import os, re
k = os.getenv("OPERATOR_PRIVATE_KEY", "")
print("length:", len(k))
print("valid hex private key:", bool(re.fullmatch(r"[0-9a-fA-F]{64}", k)))
PY
```

정상 출력:

```text
length: 64
valid hex private key: True
```

## 4. 서버 실행

아래 명령은 `<프로젝트_ROOT>` 기준으로 실행합니다.

### 4.1 백엔드 실행

```bash
cd <프로젝트_ROOT>/blockchain-backend
docker compose up -d postgres

export APP_BLOCKCHAIN_ENABLED=true
export BLOCKCHAIN_RPC_URL=https://public-en-kairos.node.kaia.io
export BLOCKCHAIN_CHAIN_ID=1001
export TRUST_TICKET_CONTRACT_ADDRESS=0x790aa2356BAb711998faA9c58dCDD47205e6683d
export BLOCKCHAIN_OPERATOR_PRIVATE_KEY=$OPERATOR_PRIVATE_KEY

./gradlew bootRun
```

정상 실행 기준:

```text
Tomcat started on port 8080
Started BackendApplicationKt
```

주의: 개발 시드 데이터가 켜져 있으면 서버 시작 시 로컬 DB가 초기화될 수 있습니다. 직접 만든 테스트 이벤트를 보존해야 한다면 `DEV_DATA_SEED=false`를 추가하고 실행하세요.

```bash
DEV_DATA_SEED=false ./gradlew bootRun
```

### 4.2 관리자 웹 실행

새 터미널에서 실행합니다.

```bash
cd <프로젝트_ROOT>/blockchain-frontend
npm install
npm run dev
```

관리자 웹:

```text
http://localhost:5173
```

기본 관리자 계정:

```text
Email: dev-admin@local.test
Password: Admin1234!
```

### 4.3 모바일 웹 실행

새 터미널에서 실행합니다.

```bash
cd <프로젝트_ROOT>/blockchain-frontend/mobile
npm install

EXPO_PUBLIC_WEB_API_BASE_URL=http://localhost:8080/api/v1 \
EXPO_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1 \
EXPO_PUBLIC_CHAIN_RPC_URL=https://public-en-kairos.node.kaia.io \
EXPO_PUBLIC_CHAIN_ID=1001 \
EXPO_PUBLIC_TRUST_TICKET_CONTRACT_ADDRESS=0x790aa2356BAb711998faA9c58dCDD47205e6683d \
EXPO_PUBLIC_FANCLUB_MEMBERSHIP_CONTRACT_ADDRESS=0xCA64026A80a9295aE1829DeDcb143dB23C3A3300 \
npx expo start --web --port 8083
```

Chrome에서 접속합니다.

```text
http://localhost:8083
```

## 5. 공통 로그인 주의사항

MetaMask 계정을 바꿀 때는 앱에서 반드시 로그아웃한 뒤 MetaMask 계정을 전환하고 다시 지갑 로그인합니다.

로그인 서명 메시지의 `지갑:` 주소가 현재 MetaMask 계정과 같은지 확인하세요.

```text
TrustTicket 로그인 요청
지갑: 0x...
nonce: ...
만료: ...
```

주소가 다르면 구매자 또는 판매자 판정이 꼬일 수 있습니다.

## 6. 시나리오 A: 1차 구매, 리셀, 체크인

### 6.1 이벤트 생성

1. Chrome 모바일 웹 `http://localhost:8083` 접속
2. MetaMask를 운영자/주최자 지갑으로 전환
3. 앱에서 지갑 로그인
4. `티켓 발매` 또는 주최자 화면 진입
5. 이벤트 생성

권장 테스트 값:

```text
이벤트명: Scenario A Main Flow
티켓 가격: 0.001 KAIA
판매 시작: 현재 시각 이전 또는 현재
판매 종료: 충분히 미래
리셀 허용: 허용
최대 리셀가: 120%
```

### 6.2 티켓 발행

1. 생성한 이벤트 상세로 이동
2. `티켓 발행` 선택
3. 티켓 1장 이상 발행
4. 발행 완료 후 잠시 기다림

백엔드는 이 시점에 다음 작업을 수행합니다.

```text
createEvent: 온체인 이벤트 생성
mintTicket: 티켓 NFT 민팅
contractEventId 저장
contractTokenId 저장
```

관리자 웹의 `블록체인 처리 현황`에서 `createEvent`, `mintTicket`이 `확정`인지 확인할 수 있습니다.

### 6.3 사용자 A 1차 구매

1. 앱에서 로그아웃
2. MetaMask를 사용자 A 지갑으로 전환
3. 지갑 로그인
4. 이벤트 목록에서 `Scenario A Main Flow` 선택
5. 좌석 선택
6. 결제 버튼 클릭
7. MetaMask 트랜잭션 승인
8. 구매 완료 화면 확인

온체인 검증:

```bash
cast call 0x790aa2356BAb711998faA9c58dCDD47205e6683d \
  "ownerOf(uint256)(address)" \
  <CONTRACT_TOKEN_ID> \
  --rpc-url https://public-en-kairos.node.kaia.io
```

결과가 사용자 A 지갑 주소여야 합니다.

### 6.4 사용자 A 리셀 등록

1. 사용자 A 로그인 상태 유지
2. `내 티켓`으로 이동
3. 방금 구매한 티켓 선택
4. 리셀 등록
5. 정가의 120% 이하 가격 입력
6. MetaMask 트랜잭션 승인
7. 리셀 등록 완료 확인

정가 120% 초과 가격으로 등록하면 컨트랙트에서 실패해야 정상입니다.

### 6.5 사용자 B 리셀 구매

1. 앱에서 로그아웃
2. MetaMask를 사용자 B 지갑으로 전환
3. 지갑 로그인
4. 리셀 목록으로 이동
5. 사용자 A가 등록한 리셀 티켓 선택
6. 구매 버튼 클릭
7. MetaMask 트랜잭션 승인
8. 구매 완료 확인

온체인 검증:

```bash
cast call 0x790aa2356BAb711998faA9c58dCDD47205e6683d \
  "ownerOf(uint256)(address)" \
  <CONTRACT_TOKEN_ID> \
  --rpc-url https://public-en-kairos.node.kaia.io
```

결과가 사용자 B 지갑 주소여야 합니다.

### 6.6 QR 체크인

1. 사용자 B 로그인 상태에서 `내 티켓` 이동
2. 리셀로 구매한 티켓 선택
3. QR 화면 진입
4. 지갑 서명 요청 승인
5. QR 생성 확인
6. 검증자 계정으로 체크인 화면 진입
7. QR 스캔 또는 티켓 검증
8. 체크인 성공 확인

체크인 이후에는 같은 티켓을 다시 사용할 수 없어야 합니다.

## 7. 시나리오 B: 팬클럽 NFT 기반 선예매

최신 UI에서는 터미널 `cast send` 없이 모바일 주최자 이벤트 상세 화면에서 Scenario B 설정을 할 수 있습니다.

### 7.1 이벤트 생성 및 티켓 발행

1. MetaMask를 운영자/주최자 지갑으로 전환
2. 앱에서 로그아웃 후 지갑 로그인
3. `티켓 발매` 진입
4. 이벤트 생성

권장 테스트 값:

```text
이벤트명: Scenario B Fanclub Presale
티켓 가격: 0.001 KAIA
판매 시작: 현재 시각 이전 또는 현재
판매 종료: 충분히 미래
총 티켓: 1장 또는 2장
```

5. 이벤트 상세에서 티켓 발행
6. 민팅 완료까지 잠시 기다림

### 7.2 팬클럽 멤버십 NFT 발급

1. 이벤트 상세 화면의 `팬클럽 선예매` 섹션으로 이동
2. `멤버십 발급 대상 지갑`에 사용자 A 지갑 주소 입력
3. `NFT 발급` 클릭
4. MetaMask에서 FanClubMembership 트랜잭션 승인
5. 발급 완료 알림 확인

이 작업을 수행하는 현재 MetaMask 지갑에는 `MEMBERSHIP_ISSUER_ROLE`이 있어야 합니다.

검증용 명령:

```bash
cast call 0xCA64026A80a9295aE1829DeDcb143dB23C3A3300 \
  "balanceOf(address)(uint256)" \
  <사용자_A_지갑주소> \
  --rpc-url https://public-en-kairos.node.kaia.io
```

결과가 `1`이면 사용자 A가 멤버십 NFT를 보유한 상태입니다.

### 7.3 멤버십 선예매 정책 적용

1. 같은 `팬클럽 선예매` 섹션에서 `선예매 적용` 클릭
2. MetaMask에서 TrustTicket 트랜잭션 승인
3. 설정 완료 알림 확인

UI는 현재 이벤트의 `contractEventId`와 실제 티켓 가격을 사용합니다. 따라서 터미널에서 가격을 잘못 입력해 `InvalidPrice()`가 발생하는 문제를 피할 수 있습니다.

이 작업을 수행하는 현재 MetaMask 지갑에는 해당 이벤트 주최자 권한 또는 `DEFAULT_ADMIN_ROLE`이 있어야 합니다.

정책은 다음 값으로 적용됩니다.

```text
enabled: true
membershipToken: FanClubMembership 주소
memberPrice: 현재 이벤트 티켓 가격
memberPresaleStart: 현재 시각 5분 전
memberPresaleEnd: 현재 시각 2시간 후
publicSaleDiscount: false
```

### 7.4 사용자 B 실패 테스트

1. 앱에서 로그아웃
2. MetaMask를 사용자 B 지갑으로 전환
3. 지갑 로그인
4. 로그인 서명 메시지 주소가 사용자 B인지 확인
5. `Scenario B Fanclub Presale` 이벤트 선택
6. 티켓 구매 시도

예상 결과:

```text
구매 실패
```

사용자 B는 FanClubMembership NFT가 없으므로 컨트랙트가 선예매 구매를 거부합니다.

### 7.5 사용자 A 성공 테스트

1. 앱에서 로그아웃
2. MetaMask를 사용자 A 지갑으로 전환
3. 지갑 로그인
4. 로그인 서명 메시지 주소가 사용자 A인지 확인
5. 같은 이벤트 선택
6. 같은 티켓 구매 시도
7. MetaMask 트랜잭션 승인
8. 구매 완료 확인

온체인 검증:

```bash
cast call 0x790aa2356BAb711998faA9c58dCDD47205e6683d \
  "ownerOf(uint256)(address)" \
  <CONTRACT_TOKEN_ID> \
  --rpc-url https://public-en-kairos.node.kaia.io
```

결과가 사용자 A 지갑 주소여야 합니다.

## 8. 자주 발생하는 문제

### 8.1 백엔드 시작 중 `NumberFormatException`

예시:

```text
NumberFormatException: For input string: "Y" under radix 16
```

원인: `BLOCKCHAIN_OPERATOR_PRIVATE_KEY`에 실제 개인키가 아니라 `YOUR_PRIVATE_KEY` 같은 placeholder가 들어갔습니다.

해결:

```bash
export OPERATOR_PRIVATE_KEY='실제_64자리_hex_개인키'
export OPERATOR_PRIVATE_KEY=${OPERATOR_PRIVATE_KEY#0x}
export BLOCKCHAIN_OPERATOR_PRIVATE_KEY=$OPERATOR_PRIVATE_KEY
```

### 8.2 `InvalidPrice()`

원인: 프론트가 보낸 결제 금액과 컨트랙트가 요구하는 가격이 다릅니다.

Scenario B는 모바일 UI의 `선예매 적용` 버튼을 사용하면 현재 티켓 가격과 같은 `memberPrice`를 자동 적용하므로 이 문제가 줄어듭니다.

### 8.3 `MembershipPassRequired()`

원인: 구매 지갑이 FanClubMembership NFT를 보유하지 않습니다.

확인:

```bash
cast call 0xCA64026A80a9295aE1829DeDcb143dB23C3A3300 \
  "balanceOf(address)(uint256)" \
  <구매자_지갑주소> \
  --rpc-url https://public-en-kairos.node.kaia.io
```

### 8.4 `ownerOf(tokenId)`가 TrustTicket 주소로 나옴

결과:

```text
0x790aa2356BAb711998faA9c58dCDD47205e6683d
```

의미: 해당 티켓은 아직 온체인에서 판매되지 않았습니다. 잘못된 `tokenId`를 확인했거나 구매 트랜잭션이 실패했을 수 있습니다.

관리자 웹 `블록체인 처리 현황`에서 `purchaseTicket`이 `확정`인지 확인하세요.

### 8.5 사용자 B인데 “본인이 등록한 리셀 티켓”이라고 표시됨

원인: 앱 세션 또는 지갑 연결 캐시가 이전 계정 상태를 유지한 경우입니다.

해결:

1. 앱에서 로그아웃
2. MetaMask 계정 전환
3. 다시 지갑 로그인
4. 로그인 서명 메시지의 지갑 주소 확인

최신 프론트엔드는 로그아웃 시 앱 토큰과 지갑 연결 캐시를 함께 정리합니다.

## 9. 발표용 핵심 문장

### 시나리오 A

```text
티켓은 단순 DB 데이터가 아니라 ERC721 NFT로 발행됩니다.
구매와 리셀은 컨트랙트가 직접 처리하고, 리셀 가격 상한과 임의 양도 제한도 온체인에서 검증합니다.
```

### 시나리오 B

```text
팬클럽 멤버십 NFT는 티켓 플랫폼이 직접 관리하는 DB 권한이 아니라 외부 컨트랙트가 발급한 자격입니다.
TrustTicket 컨트랙트는 구매 시점에 해당 NFT 보유 여부를 확인하여 멤버십 보유자에게만 선예매 권한을 부여합니다.
```
