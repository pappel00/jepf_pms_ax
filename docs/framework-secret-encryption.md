# 설정값(토큰·비밀번호) 암호화

`application.yml` 에 적는 비밀값(봇 토큰, SMTP 비밀번호 등)을 `ENC(...)` 암호문으로 저장하고, 서버가 시작할 때 자동으로 복호화한다.
framework-core 공통 기능이라 모든 모듈·앱이 같은 방식으로 쓴다(`com.platform.framework.core.crypto`).

## 방식
- **AES-256-GCM** — 값마다 새 솔트·IV 를 쓰고, 마스터 키에서 PBKDF2(HmacSHA256, 12만 회)로 암호키를 만든다. GCM 태그로 변조도 감지한다.
- 같은 값을 암호화해도 매번 다른 `ENC(...)` 가 나온다.
- 코드는 평소처럼 `@Value`·`environment.getProperty` 로 읽는다(평문을 받음). 환경변수로 평문을 주면 그 값이 우선한다.
- `ENC(...)` 가 있는데 마스터 키가 없거나 틀리면 서버 시작이 중단되고 대상 설정 이름을 안내한다(값은 출력하지 않는다).

## 마스터 키 — 서버마다 한 번
소스·저장소에는 두지 않는다. 아래 중 하나로 둔다(위에서부터 찾는다).

1. 환경변수 `JEPF_SECRET_KEY`
2. 키 파일 — 환경변수 `JEPF_SECRET_KEY_FILE` 이 가리키는 파일, 없으면 사용자 홈의 `.jepf/secret.key` (첫 줄)

개발 PC 는 도구로 만들면 `~/.jepf/secret.key` 가 생긴다. **개발·운영 서버에는 같은 키를 환경변수나 키 파일로 한 번 넣어야** 복호화된다.
키가 달라지면 기존 `ENC(...)` 는 풀리지 않으므로 키는 안전한 곳에 백업한다.

## 도구
```bash
./gradlew -q :framework-core:secretTool --args="generate-key"   # 마스터 키 생성(키 파일 저장, 이미 있으면 거부)
./gradlew -q :framework-core:secretTool --args="encrypt"        # 입력한 값을 암호화 → ENC(...) 출력
./gradlew -q :framework-core:secretTool --args="decrypt"        # ENC(...) 복호화(확인용)
```
`encrypt`/`decrypt` 는 값을 명령줄 인자가 아니라 **표준입력 한 줄**로 받아 셸 기록에 남지 않는다. 실행하면 입력 안내가 나오고, 값을 입력한 뒤 Enter.

## yml 에 적기
```yaml
pms:
  notify:
    email:
      password: ${PMS_SMTP_PASSWORD:ENC(AbC...)}   # 환경변수가 있으면 그 평문이 우선, 없으면 ENC 복호화
```

## 주의
- 이미 평문으로 노출된 적이 있는 값은 암호화하더라도 재발급을 권한다.
- 마스터 키 파일(`secret.key`)은 저장소에 커밋하지 않는다.
