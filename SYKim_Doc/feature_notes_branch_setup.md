# feature_notes 브랜치 생성 가이드

`feature_notes` 브랜치는 ERD에 정의된 노트 관련 테이블과 CRUD 기능을 구현하기 위한 작업 공간입니다. 아래 절차대로 브랜치를 만들고 원격 저장소에 공유한 뒤, 문서 확인과 승인을 거쳐 개발을 시작합니다.

## 1. 사전 확인
1. 현재 작업 트리를 점검합니다.
   ```bash
   git status
   ```
   - 변경 사항이 존재한다면 커밋하거나 임시로 스태시(`git stash push`)한 뒤 다음 단계로 진행합니다.
2. 메인 브랜치(`main`)가 체크아웃되어 있는지 확인합니다.
   ```bash
   git branch --show-current
   ```
   - `main`이 아니라면 `git switch main` 명령으로 전환합니다.

## 2. 메인 브랜치 최신화
1. 원격 저장소 정보를 갱신합니다.
   ```bash
   git fetch origin
   ```
2. 로컬 `main` 브랜치를 최신 상태로 fast-forward 합니다.
   ```bash
   git pull --ff-only origin main
   ```
   - merge 커밋 없이 최신 상태로 맞추기 위해 `--ff-only` 옵션을 사용합니다. 충돌이 발생하면 충돌을 해결한 뒤 다시 수행합니다.

## 3. feature_notes 브랜치 생성
1. 새 브랜치를 생성하고 동시에 체크아웃합니다.
   ```bash
   git switch -c feature_notes
   ```
   - 기존에 동일한 이름의 브랜치가 있다면 먼저 삭제하거나 다른 이름을 사용합니다.
2. 생성이 완료되었는지 확인합니다.
   ```bash
   git status
   ```
   - 출력에 `On branch feature_notes`가 표시됩니다.

## 4. 원격 브랜치 등록
1. 새 브랜치를 원격 저장소에 업로드하고 추적 관계를 설정합니다.
   ```bash
   git push -u origin feature_notes
   ```
2. 원격에 브랜치가 생성되었는지 확인합니다.
   ```bash
   git branch -r | grep feature_notes
   ```
   - `origin/feature_notes`가 보이면 성공입니다.

## 5. 다음 단계
- 위 절차가 완료되면 이 문서를 기준으로 작업 시작 여부를 공유하고 승인을 받습니다.
- 승인이 나면 `feature_notes` 브랜치에서 노트 관련 DB 스키마와 기본 CRUD 구현을 진행합니다.
- 추가 작업 중간에도 주기적으로 `git fetch --all` 및 `git merge origin/main` 또는 `git rebase origin/main`으로 메인 변경 사항을 반영하세요.

> ⚠️ 주의: 현재 문서 승인 전까지는 스키마 및 코드 수정 작업을 시작하지 않습니다.
