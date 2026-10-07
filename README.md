# Order Management Platform

[![CI](https://github.com/saiharshith/order-management-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/saiharshith/order-management-platform/actions/workflows/ci.yml)

A Spring Boot order management API, built as a hands-on system design playbook rather than a single-shot project. Each commit in this repo introduces exactly one production concept — persistence, authentication, authorization, caching, validation, observability, testing, and containerization — with the reasoning and trade-offs behind it kept in the commit message itself. The commit history is the actual documentation of *why* things are built the way they are, not just *what* was added.

The project is deliberately evolving in phases:

- **Phase 1 (this repo, complete):** a modular monolith covering the fundamentals below.
- **Phase 2 (planned):** decompose into Kafka-driven microservices (Order, Inventory, Payment, Notification), introducing event-driven architecture, the saga pattern, and distributed tracing.
- **Phase 3 (planned):** an MCP server exposing this platform's APIs as tools for AI agents.

## What's implemented

- **REST API** for order management — full CRUD under `/api/orders`
- **Persistence** via Spring Data JPA + PostgreSQL (Dockerized)
- **Authentication** via Spring Security + JWT (registration, login, stateless token validation)
- **Authorization** — role-based access control (`USER` / `ADMIN`) via method-level `@PreAuthorize`
- **Validation** — Jakarta Bean Validation with a clean, structured 400 response format
- **Caching** — Spring's cache abstraction backed by Redis (business logic is fully decoupled from the cache provider)
- **Observability** — Spring Boot Actuator (health/metrics/Prometheus), MDC-based request correlation IDs threaded through every log line
- **Testing** — JUnit 5 + Mockito unit tests, and Testcontainers-backed integration tests exercising the real HTTP layer against ephemeral Postgres and Redis containers
- **Containerization** — multi-stage Dockerfile, full `docker-compose.yml` stack (app + Postgres + Redis) with healthcheck-gated startup ordering
- **CI** — GitHub Actions running the full test suite and a Docker build validation on every push

## Tech stack

Java 21 · Spring Boot 4.1 · Spring Data JPA · Spring Security · Spring Cache · PostgreSQL · Redis · JWT (jjwt) · Jakarta Bean Validation · Spring Boot Actuator + Micrometer/Prometheus · JUnit 5 + Mockito · Testcontainers · Docker & Docker Compose · GitHub Actions

## Getting started

### Prerequisites

- Java 21 (the included `mvnw` wrapper handles the exact Maven version — no separate Maven install needed)
- Docker / Docker Compose (or a compatible alternative such as Rancher Desktop)

### 1. Configure environment variables

```bash
cp .env.example .env
```

Edit `.env` and set real values for `JWT_SECRET`, `ADMIN_PASSWORD`, etc. — the defaults in `.env.example` are placeholders, not meant to be used as-is.

### 2. Run the full stack (app + Postgres + Redis) with Docker Compose

```bash
docker-compose up --build
```

The API is then available at `http://localhost:8080`.

### 3. Or, run the app locally against containerized dependencies

Useful for active development with hot-reload from an IDE:

```bash
docker-compose up -d postgres redis
./mvnw spring-boot:run
```

Both approaches connect to the same Postgres/Redis containers — the app's datasource/cache host configuration (`POSTGRES_HOST`, `REDIS_HOST`) defaults to `localhost` for this workflow and is overridden to the Compose service names only when the app itself runs inside `docker-compose`.

## API overview

| Method | Endpoint | Auth required | Notes |
|---|---|---|---|
| POST | `/api/auth/register` | No | Creates a `USER`-role account; returns 409 on duplicate username |
| POST | `/api/auth/login` | No | Returns a JWT on success |
| GET | `/api/orders` | Yes | Any authenticated user |
| GET | `/api/orders/{id}` | Yes | Any authenticated user |
| POST | `/api/orders` | Yes | Any authenticated user |
| PUT | `/api/orders/{id}` | Yes, `ADMIN` | Regular users receive 403 |
| DELETE | `/api/orders/{id}` | Yes, `ADMIN` | Regular users receive 403 |
| GET | `/actuator/health` | No (basic) / `ADMIN` (detailed) | Postgres + Redis component status |
| GET | `/actuator/metrics`, `/actuator/prometheus` | `ADMIN` | Micrometer metrics |

A default admin user (`ADMIN_USERNAME`/`ADMIN_PASSWORD` from `.env`) is seeded automatically on startup.

## Exploring the API

An importable **Postman collection** (`order-management-platform.postman_collection.json`) is included, covering the full flow: registration/login, the 401-vs-403 distinction between missing auth and insufficient role, validation errors, admin-only actions, correlation ID propagation, and the observability endpoints. Admin credentials are stored as collection variables, not hardcoded — fill in your own `.env` values before running it.

## Testing

```bash
./mvnw test
```

Integration tests use Testcontainers to spin up disposable Postgres and Redis containers automatically — no need for the dev `docker-compose` containers to already be running.

**Note for Rancher Desktop users:** Testcontainers may need these environment variables set locally, since Rancher Desktop doesn't expose the Docker socket at the default path and its Ryuk cleanup sidecar can fail to start under its VM:

```bash
export DOCKER_HOST=unix://$HOME/.rd/docker.sock
export TESTCONTAINERS_RYUK_DISABLED=true
```

This is not required in CI — GitHub-hosted runners have a standard Docker daemon.

## Project structure

```
src/main/java/io/github/saiharshith/ordermanagementplatform/
├── auth/     # User, roles, JWT issuance/validation, Spring Security config
├── config/   # Caching, correlation ID filter, cross-cutting configuration
└── order/    # Order domain: entity, repository, service, controller, request DTOs
```

## Commit history

Every enhancement in this repo landed as its own commit, in the order it was actually built — starting from a bare in-memory REST API and layering in persistence, auth, caching, validation, observability, testing, and containerization one deliberate step at a time. Reading `git log` top to bottom is effectively a narrated build log of a production-style Spring Boot application, including a couple of real bugs found and fixed along the way (see the "Fix Create Order incorrectly requiring status" commit for one caught through manual exploration rather than automated tests).
