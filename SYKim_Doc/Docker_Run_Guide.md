# Docker 기반 서버 실행 가이드

현재 `server-backend` 애플리케이션과 프리티어 의존 서비스(Postgres, Redis)를 Docker로 기동하기 위한 절차입니다. 최초 1회 대비 과정과 반복 실행 시 필요한 단계로 나누어 설명합니다.

## 1. 사전 준비

1. Docker 및 Docker Compose(플러그인 포함)가 설치되어 있어야 합니다.
2. 환경 변수 파일 준비  
   - 프로젝트 루트(`Server-Backend/`)에 `.env`가 없으면 `.env-example`을 복사하여 필요한 값을 채웁니다.  
     ```bash
     cp .env-example .env
     ```
3. 외부 네트워크 생성(최초 1회만 실행)  
   - 이미 존재한다면 생략합니다.
     ```bash
     docker network create artbite-net
     docker network create elk-net
     ```

## 2. 컨테이너 실행

프로젝트 루트에서 다음 명령을 실행하면 애플리케이션, Postgres, Redis 세 컨테이너가 동시에 기동됩니다.

```bash
  docker compose -f docker-compose.yml -f infra/database/docker-compose.yml up -d
```
```bash
    [+] Running 3/3
    ✔ Container artbite-redis     Healthy                                                                                                                                        5.8s
    ✔ Container artbite-postgres  Healthy                                                                                                                                        5.8s
    ✔ Container artbite-app       Started
```
- `docker-compose.yml` : 애플리케이션(`app`) 정의  
- `infra/database/docker-compose.yml` : Postgres(`artbite-postgres`), Redis(`artbite-redis`) 정의

## 3. 상태 확인

서비스가 정상적으로 올라왔는지 확인하려면 아래 명령을 사용합니다.

```bash
  docker compose -f docker-compose.yml -f infra/database/docker-compose.yml ps
```

- `artbite-postgres`, `artbite-redis` 는 `healthy` 상태가 되어야 합니다.
- `artbite-app` 은 Health Check(`http://localhost:8080/actuator/health`)가 통과하면 `healthy` 로 전환됩니다.
  - 로그 확인: `docker logs artbite-app`
  - Redis 로그 확인: `docker logs artbite-redis`
  ```bash
  [+] Running 3/3
    ✔ Container artbite-postgres  Healthy                                                                                                                                        5.6s
    ✔ Container artbite-redis     Healthy                                                                                                                                        5.6s
    ✔ Container artbite-app       Started
  ```
## 4. 서비스 중지 및 정리

실행 중인 컨테이너를 중지하고 정리하려면 다음 명령을 실행합니다.

```bash
  docker compose -f docker-compose.yml -f infra/database/docker-compose.yml down
```
```bash
  [+] Running 3/3
 ✔ Container artbite-app       Removed                                                                                                                                        0.4s 
 ✔ Container artbite-postgres  Removed                                                                                                                                        0.2s 
 ✔ Container artbite-redis     Removed  
  ```
- 위 명령은 컨테이너, 네트워크(외부 네트워크 제외), 볼륨을 정리합니다.
- 추가 볼륨 정리가 필요하면 `docker volume ls`, `docker volume rm <이름>` 을 활용합니다.

## 5. 자주 발생하는 문제와 해결

- `artbite-app` 이미지가 없다는 오류: `docker compose` 실행 시 빌드가 자동으로 진행되므로 실패 이유를 로그에서 확인하고, 필요한 경우 로컬 빌드(`./gradlew build`) 또는 Docker 빌드 컨텍스트를 점검합니다.
- 네트워크 관련 오류: `artbite-net`, `elk-net` 존재 여부를 먼저 확인하고, 필요하다면 삭제 후 재생성합니다.  
  ```bash
  docker network ls
  docker network rm artbite-net elk-net
  docker network create artbite-net
  docker network create elk-net
  ```

## 6. 도커 실행 및 정지 
- 도커 실행 
 ```bash
    docker compose -f docker-compose.yml -f infra/database/docker-compose.yml start
  ```
- 도커 중지
 ```bash
    docker compose -f docker-compose.yml -f infra/database/docker-compose.yml stop
  ```

위 단계대로 진행하면 사용자가 직접 Docker 기반 서버와 Redis 서비스를 실행/정지할 수 있습니다.
