# SmartTrace — High-Throughput Label Generation & Ingestion

SmartTrace is a high-performance logistics label generation and ingestion system designed to generate hierarchical identifiers (pallets → cartons → units), compute HMAC-based integrity hashes, and stream them into PostgreSQL using `COPY` for maximum throughput and minimal synchronization overhead.

---

## Performance

### SLA vs Achieved

| Metric | SLA | Achieved |
|--------|-----|----------|
| **10k labels** (pallets + cartons + units) | &lt; 5 seconds | **~100 ms** |
| **5.51M labels** (generation + hashing + insertion) | — | **~7 seconds** |
| **Hashing only** (5.51M labels) | — | **~5 seconds** |
| **End-to-end throughput** | — | **~787K labels/sec** |

### Summary

- **10k labels** in ~100 ms — **50× faster** than the 5-second SLA
- **5.51M labels** in ~7 seconds — full pipeline (generate → hash → insert)
- HMAC-SHA256 hashing is CPU-bound (~5s of the 7s total)
- Embarrassingly parallel; scales with core count

---

## Architecture

### Core Goals

- Ultra-fast label generation
- Cryptographic integrity via HMAC-SHA256 hashing
- Streaming ingestion with PostgreSQL `COPY`
- Minimal synchronization overhead
- Maximum CPU parallelism via partitioned pipelines

---

## Architectural Evolution

### Version 1 — Queue-Based Batch Pipeline

**Structure:** Multiple Producers → Shared `BlockingQueue` → Multiple Consumers → Batches (50k) → Insert

**Issues:**

- Lock contention on shared queue
- Cache-line contention
- Excess object allocation
- Late serialization
- Central coordination bottleneck

**Result:** ~25 seconds for full workload

---

### Version 2 — Partitioned Streaming Pipeline (Current)

**Structure per worker:**

```
Generate → Hash → Serialize → Stream → PostgreSQL COPY
```
## How Generation Works

### Pipeline Overview

> **Three stages:** Pallets (producer) → Cartons (consumer–producer) → Units (consumer). Each stage has **5 workers**. Each worker has its own pipe; the DB side reads from it and runs `COPY` in parallel. **15 total COPY connections** (5 per table).

| Stage | Role | Producers | Consumers | Queue |
|-------|------|-----------|-----------|-------|
| **Pallets** | Generate pallet IDs | 5 pallet workers | 5 carton workers | `palletQueue` (200k) |
| **Cartons** | Generate carton IDs per pallet | 5 carton workers | 5 unit workers | `cartonQueue` (200k) |
| **Units** | Generate unit IDs per carton | — | 5 unit workers | — |

---

### Hierarchical ID Structure

```
Pallet (SSIC)  →  Carton (SSIC_<n>)  →  Unit (SSIC_<n>_<m>)
```

| Level | Format | Example |
|-------|--------|---------|
| **Pallet** | `companyPrefix + factoryId + employeeId + timestamp + index` | `ACME_F1_E001_20250110120000_0` |
| **Carton** | `parentPalletId + "_" + cartonIndex` | `ACME_F1_E001_20250110120000_0_1` |
| **Unit** | `parentCartonId + "_" + unitIndex` | `ACME_F1_E001_20250110120000_0_1_1` |

> Parents must be produced before children. **Queues enforce ordering:** pallets → `palletQueue` → cartons → `cartonQueue` → units.

---

### Pallet Partitioning (No Shared Work)

```java
chunk = (noOfPallets + 5 - 1) / 5
Worker 0: [0, chunk), Worker 1: [chunk, 2*chunk), ...
```

> Each pallet worker gets a **non-overlapping index range**. No queue among pallet workers; they produce in parallel and put results into `palletQueue` for carton workers.

---

### Producer–Consumer Stop: Poison Pills

> **`POISON`** = `static final byte[] {-1}`. Carton/unit workers loop on `take()` and stop when they receive this **reference** (`==` comparison).

| Step | Action |
|------|--------|
| 1 | Pallets finish → `cfallpalets.join()` |
| 2 | **Put 5 poison pills** into `palletQueue` (one per carton worker) |
| 3 | Cartons finish → `cfallcartons.join()` |
| 4 | **Put 5 poison pills** into `cartonQueue` (one per unit worker) |
| 5 | Units finish → `cfallunits.join()` |
| 6 | All DB COPY tasks finish → `alldbs.join()` |

> Carton/unit workers don’t know the total count; they only know “take next item.” Poison pills signal **end of stream**.

---

### Data Flow

```
┌──────────────────┐     palletQueue      ┌──────────────────┐    cartonQueue    ┌──────────────────┐
│ Pallet Worker 0  │ ──────────────────▶  │ Carton Worker 0  │ ───────────────▶  │  Unit Worker 0   │
│ Pallet Worker 1  │ ──────────────────▶  │ Carton Worker 1  │ ───────────────▶  │  Unit Worker 1   │
│ ...              │         ...          │ ...              │        ...        │  ...             │
│ Pallet Worker 4  │ ──────────────────▶  │ Carton Worker 4  │ ───────────────▶  │  Unit Worker 4   │
└────────┬─────────┘                      └────────┬─────────┘                   └────────┬─────────┘
         │                                         │                                      │
         ▼                                         ▼                                      ▼
   PipedOutputStream[i]                    PipedOutputStream[i]                   PipedOutputStream[i]
         │                                         │                                      │
         ▼                                         ▼                                      ▼
   COPY pallets                            COPY cartons                             COPY units
```

---

### Worker Pseudocode

**Pallet worker**

```
for i in [startIdx, endIdx):
  payload = prefix + i
  hash = HMAC(payload)
  palletQueue.put(payload)
  write payload, hash, hash_prefix, job_id to pipe
close pipe
```

**Carton worker**

```
loop:
  parent = palletQueue.take()
  if parent == POISON → break
  for i = 1..cartonsPerPallet:
    payload = parent + "_" + i
    hash = HMAC(payload)
    cartonQueue.put(payload)
    write payload, parent, hash, hash_prefix, job_id to pipe
close pipe
```

**Unit worker**

```
loop:
  parent = cartonQueue.take()
  if parent == POISON → break
  for i = 1..unitsPerCarton:
    payload = parent + "_" + i
    hash = HMAC(payload)
    write payload, parent, hash, hash_prefix, job_id to pipe
close pipe
```

---

**Properties:**

- Independent pipelines (no shared hot structures)
- Dedicated stream per worker via `PipedOutputStream` / `PipedInputStream`
- Dedicated `COPY` connection per worker
- Implicit backpressure via pipe buffering
- Virtual threads + `CompletableFuture` for parallelism

**Result:** ~25s → **~7s** (~3.5× improvement)

---

## Why Streaming Instead of BlockingQueue

- Removes shared synchronization hotspots
- Eliminates intermediate batching and object churn
- Reduces memory pressure
- Improves CPU cache locality
- Aligns with PostgreSQL streaming `COPY` ingestion

---

## Pipeline Model

Each worker performs:

1. **Label generation** — Hierarchical IDs (`companyPrefix + factoryId + employeeId + timestamp + index`)
2. **HMAC hashing** — HmacSHA256 for integrity verification
3. **Serialization** — Byte-buffer writes to output stream
4. **Streaming ingestion** — PostgreSQL `COPY FROM STDIN` (CSV)

Design: avoid centralized coordination; prefer independent execution lanes per pallet/carton/unit type.

---



### Summary

| Aspect | Detail |
|--------|--------|
| **Pallet workers** | 5 producers, partitioned by index, write to `palletQueue` + pipe |
| **Carton workers** | 5 consumer–producers, `palletQueue` → `cartonQueue` + pipe |
| **Unit workers** | 5 consumers, `cartonQueue` → pipe |
| **DB workers** | 15 `COPY` tasks (5 per table), each from one pipe |
| **Stop mechanism** | Pallet: fixed range; Carton/Unit: poison pill per worker |
| **Hierarchy** | Enforced via queues: pallet → carton → unit |

---


## SQL Schemas

Create the following tables before running the application. The project uses `spring.jpa.hibernate.ddl-auto=none`; tables must exist.

### `pallets`

```sql
CREATE TABLE pallets (
    ssic          BYTEA    NOT NULL,
    hash          BYTEA    NOT NULL,
    hash_prefix   BYTEA    NOT NULL,
    job_id        BYTEA    NOT NULL,
    is_scanned    BOOLEAN  DEFAULT FALSE,
    PRIMARY KEY (ssic)
);

CREATE INDEX idx_pallets_job_id      ON pallets (job_id);
CREATE INDEX idx_pallets_hash_prefix ON pallets (hash_prefix);
CREATE INDEX idx_pallets_hash        ON pallets (hash);
```

### `cartons`

```sql
CREATE TABLE cartons (
    serial_id         BYTEA    NOT NULL,
    parent_pallet_id  BYTEA    NOT NULL,
    hash              BYTEA    NOT NULL,
    hash_prefix       BYTEA    NOT NULL,
    job_id            BYTEA    NOT NULL,
    is_scanned        BOOLEAN  DEFAULT FALSE,
    PRIMARY KEY (serial_id)
);

CREATE INDEX idx_cartons_job_id           ON cartons (job_id);
CREATE INDEX idx_cartons_parent_pallet_id ON cartons (parent_pallet_id);
CREATE INDEX idx_cartons_hash_prefix      ON cartons (hash_prefix);
CREATE INDEX idx_cartons_hash             ON cartons (hash);
```

### `units`

```sql
CREATE TABLE units (
    serial_id         BYTEA    NOT NULL,
    parent_carton_id  BYTEA    NOT NULL,
    hash              BYTEA    NOT NULL,
    hash_prefix       BYTEA    NOT NULL,
    job_id            BYTEA    NOT NULL,
    is_scanned        BOOLEAN  DEFAULT FALSE,
    PRIMARY KEY (serial_id)
);

CREATE INDEX idx_units_job_id           ON units (job_id);
CREATE INDEX idx_units_parent_carton_id ON units (parent_carton_id);
CREATE INDEX idx_units_hash_prefix      ON units (hash_prefix);
CREATE INDEX idx_units_hash             ON units (hash);
```

### `factory`

```sql
CREATE TABLE factory (
    id           SERIAL  PRIMARY KEY,
    company_name VARCHAR(255),
    location     VARCHAR(255),
    manager_id   INTEGER REFERENCES employee(employee_id)
);
```

### `employee`

```sql
CREATE TABLE employee (
    employee_id SERIAL  PRIMARY KEY,
    first_name  VARCHAR(255),
    last_name   VARCHAR(255),
    email       VARCHAR(255) UNIQUE,
    password    VARCHAR(255),
    role        VARCHAR(50),
    factory_id  INTEGER REFERENCES factory(id)
);

-- Add FK from factory to employee after both tables exist
ALTER TABLE factory
    ADD CONSTRAINT fk_factory_manager
    FOREIGN KEY (manager_id) REFERENCES employee(employee_id);
```

**Note:** `factory` and `employee` reference each other; create `employee` first without the `factory_id` FK if needed, or use deferred constraints. The COPY pipeline uses only `pallets`, `cartons`, and `units`.

---

## Tech Stack

- **Runtime:** Java 21+, Spring Boot 3.x
- **DB:** PostgreSQL, JDBC `CopyManager` for streaming
- **Crypto:** HmacSHA256 (javax.crypto)
- **Concurrency:** Virtual threads, `CompletableFuture`, `ExecutorService`

---

## API Reference

### Auth

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/login` | Public | Login with email/password; returns JWT |
| POST | `/auth/register` | MANAGER | Register employee (requires factory) |

### Label Generation

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/products/generate` | MANAGER | Fire-and-forget bulk generation; returns immediately |
| POST | `/products/generateImmediate` | EMPLOYEE | Generate, then fetch all labels by `job_id` |

### Scanning (Hierarchical)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/scan/pallet/prefix` | EMPLOYEE | Scan pallet by `hash_prefix` |
| POST | `/scan/carton/prefix` | EMPLOYEE | Scan carton by `hash_prefix` (pallet must be scanned first) |
| POST | `/scan/unit/prefix` | EMPLOYEE | Scan unit by `hash_prefix` (carton must be scanned first) |

### Verification

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/verify` | Public | Verify product authenticity by full hash (unit/carton/pallet) |

---

## License

Proprietary.
