# NetWalk - Project Setup

## Deploy

``` sh
cd backend && docker compose up -d --build

cd frontend && npm install && npm run build    # output in frontend/dist/

cd androidApp && ./gradlew assembleDebug       # output in androidApp/app/build/outputs/apk/debug/app-debug.apk
```

---

## Prerequisites

### Required for everyone

- [**just**](https://github.com/casey/just#installation)
    (or `uv tool install just`)
- [**pre-commit**](https://pre-commit.com/#install)
    (or `uv tool install pre-commit`)
- [**Docker CE + Compose**](https://docs.docker.com/engine/install/)

### Backend only (python)

- [**uv**](https://docs.astral.sh/uv/getting-started/installation/)
- [**jq**](https://github.com/jqlang/jq#installation)
    (or `sudo apt install jq`)

### Frontend only (node.js)

- [**Node.js + npm**](https://nodejs.org/)
    (or via [volta](https://docs.volta.sh/guide/getting-started))

### Android Only

- [**Gradle**](https://gradle.org/install/)
    (or via [sdkman](https://sdkman.io/))
- [**Android SDK**](https://developer.android.com/studio#command-tools )
    (or via [sdkman](https://sdkman.io/))

> **Android note**: `gradle-wrapper.jar` is NOT committed. If missing, run:

``` sh
just androidApp wrapper  # Requires gradle in PATH
```

---

## Start

### One-time: Install git hooks

``` sh
just install-hooks
```

> **Note**: with pre-commit hooks your commit may fail
> and you will need to re-stage changes after formatting!

### Backend

``` sh
just backend makeenv      # (first-time only) copy .env.example -> .env
just backend install      # (first-time only) uv sync --all-extras
just backend db-up        # start database container
just backend serve-host   # run FastAPI locally with hot-reload
```

### Frontend

``` sh
just frontend install     # npm install
just frontend dev         # npm run dev (hot-reload)
```

### Android

``` sh
just androidApp wrapper   # (first-time only) regenerate gradle wrapper
just androidApp assemble  # build project
```

---

## Common commands

### Backend commands

``` sh
just backend check        # ruff check
just backend lint         # ruff format
just backend db-shell     # psql shell
just backend test-all     # API tests
just backend logs-api     # view container logs
just backend serve-docker # run API in docker (DB must be already running)
just backend up-full      # start API + DB in docker
```

### Frontend commands

``` sh
just frontend build       # production build
just frontend install     # reinstall deps
```

### Android commands

``` sh
just androidApp lint      # ktlintCheck
just androidApp format    # ktlintFormat
just androidApp build     # build final APK (not ready yet)
```
