# Repository Guidelines

## Project Structure & Module Organization
`dy05` is a Spring Boot + React monorepo. The backend is a **Maven multi-module reactor**: root `pom.xml` (packaging `pom`) lists **`douyin-operations-contract`**, **`douyin-operations-common`**, **`douyin-operations-platform`**, **`douyin-operations-integration`**, **`douyin-operations-asset`**, **`douyin-operations-content`**, **`douyin-operations-intelligence`**, **`douyin-operations-douyin`**, **`douyin-operations-payment`**, **`douyin-operations-live`**, **`douyin-operations-shortvideo`**, and **`douyin-operations-app`**. Domain code lives under each module’s `src/main/java` (same `cn.gaifan.douyinOperations.module.*` packages); **`douyin-operations-app`** is the **executable** module (main class, Flyway, cross-domain glue). Resources and Flyway migrations live in **`douyin-operations-app/src/main/resources/`**, especially `db/migration/`. Tests: many integration tests remain under **`douyin-operations-app/src/test`**; unit tests may also live beside code in other modules.

Frontend code is in `front/src`, organized by `api`, `components`, `hooks`, `pages`, `stores`, and `types`. Unit tests live beside source files or in `front/src/__tests__`; Playwright suites live in `front/e2e/tests`. Deployment files are in `docker/`, and longer design notes are in `docs/`.

## Build, Test, and Development Commands
Run backend commands from the repo root:

- `mvn compile` checks Java compilation.
- `mvn -pl douyin-operations-app -am spring-boot:run` starts the backend on `localhost:8080`（根 POM 为 `packaging=pom`，勿裸跑 `mvn spring-boot:run`；或仓库根执行 **`run-app.cmd`**）。
- `mvn test` runs JUnit tests.
- `mvn verify` runs the Maven verification pipeline.

Run frontend commands from `front/`:

- `npm run dev` starts Vite on `localhost:3000`.
- `npm run build` runs TypeScript checks and creates a production build.
- `npm run test` runs Vitest once.
- `npm run test:coverage` generates coverage reports.
- `npm run test:e2e` runs Playwright suites.

## Coding Style & Naming Conventions
Use Java 17 and keep the existing Spring layering: `*Controller`, `*Service`, `*Repository`, and `*VO`. Java uses 4-space indentation and `UpperCamelCase` type names. Keep package paths aligned with modules, for example `module/live/service/`. Frontend code uses React 18 + TypeScript, 2-space indentation, `PascalCase` for components/pages, and `camelCase` for hooks and utilities. No formatter config is committed, so match the surrounding file exactly.

## Testing Guidelines
Backend tests use JUnit 5, Mockito, AssertJ, and JaCoCo. Name unit tests `*Test.java` and integration tests `*IntegrationTest.java`. Frontend unit tests use Vitest with React Testing Library and should use `*.test.ts(x)` or `*.spec.ts(x)`. Playwright specs use `*.spec.ts` under `front/e2e/tests`. The current frontend coverage config targets 100% thresholds, so new code should include tests before merge.

## Commit & Pull Request Guidelines
Recent history follows `type(scope): summary`, for example `feat(benchmark): ...`, `test(benchmark): ...`, and `docs(benchmark): ...`. Keep scopes module-specific and summaries short. PRs should include a clear description, linked issue or planning doc, commands run for validation, and screenshots for UI work. Call out schema, env, or API contract changes explicitly.

## Security & Configuration Tips
Do not commit secrets or local overrides such as `.env`, `docker/.env`, or `sql/config/ai-keys-local.sql`. Avoid checking in generated artifacts like `front/dist`, `front/node_modules`, logs, or archives such as `pom.zip`.

## UI & Design References
For new pages or visual polish, you may copy a `DESIGN.md` from [awesome-design-md](https://github.com/VoltAgent/awesome-design-md) into the repo root and follow it together with existing MUI patterns in `front/src`.
