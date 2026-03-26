# FinFlow Event Catalog

This document describes all Kafka events produced and consumed by the FinFlow system.

---

## Topic: `transaction-events`

### TRANSACTION_COMPLETED

| Field | Value |
|-------|-------|
| **Producer** | `TransactionService` (transaction module) |
| **Consumer** | `FraudConsumer` (fraud module) |
| **Trigger** | Transfer başarıyla tamamlandığında |
| **Partition Key** | `sourceAccountId` — aynı hesabın işlemleri aynı partition'a gider (sıralama garantisi) |

**Payload:**

```json
{
  "transactionId": "uuid",
  "sourceAccountId": "uuid",
  "targetAccountId": "uuid",
  "amount": "500.0000",
  "currency": "CHF",
  "status": "COMPLETED",
  "description": "string or null",
  "timestamp": "2026-03-25T10:00:00"
}
```

| Alan | Tip | Açıklama |
|------|-----|----------|
| `transactionId` | UUID | İşlemin benzersiz ID'si |
| `sourceAccountId` | UUID | Gönderen hesap ID'si |
| `targetAccountId` | UUID | Alıcı hesap ID'si |
| `amount` | BigDecimal | Transfer tutarı (4 decimal) |
| `currency` | String (3) | ISO 4217 para birimi kodu |
| `status` | String | `COMPLETED` |
| `description` | String | Opsiyonel açıklama |
| `timestamp` | LocalDateTime | Event oluşturulma zamanı |

---

### TRANSACTION_FAILED

| Field | Value |
|-------|-------|
| **Producer** | `TransactionService` (transaction module) |
| **Consumer** | `FraudConsumer` (fraud module) |
| **Trigger** | Transfer başarısız olduğunda (yetersiz bakiye vb.) |
| **Partition Key** | `sourceAccountId` |

**Payload:** `TRANSACTION_COMPLETED` ile aynı format; `status` alanı `FAILED` değerini taşır.

```json
{
  "transactionId": "uuid",
  "sourceAccountId": "uuid",
  "targetAccountId": "uuid",
  "amount": "500.0000",
  "currency": "CHF",
  "status": "FAILED",
  "description": "string or null",
  "timestamp": "2026-03-25T10:00:00"
}
```

---

## Topic: `fraud-alerts`

### FRAUD_ALERT

| Field | Value |
|-------|-------|
| **Producer** | `FraudService` (fraud module) |
| **Consumer** | *(ileride notification module)* |
| **Trigger** | Şüpheli işlem tespit edildiğinde |
| **Partition Key** | `transactionId` |

**Payload:**

```json
{
  "fraudCaseId": "uuid",
  "transactionId": "uuid",
  "ruleViolated": "HIGH_AMOUNT_TRANSFER",
  "riskScore": 85,
  "timestamp": "2026-03-25T10:00:00"
}
```

| Alan | Tip | Açıklama |
|------|-----|----------|
| `fraudCaseId` | UUID | Fraud vakasının benzersiz ID'si |
| `transactionId` | UUID | Şüpheli işlemin ID'si |
| `ruleViolated` | String | İhlal edilen kural adı |
| `riskScore` | int | Risk skoru (0–100) |
| `timestamp` | LocalDateTime | Tespit zamanı |

---

## Event Akışı

```
TransactionService
      │
      ├─► [outbox_events table]
      │         │
      │    OutboxPublisher (her 500ms)
      │         │
      │         ▼
      │   transaction-events (Kafka)
      │         │
      │         ▼
      │   FraudConsumer
      │         │
      │    FraudService
      │         │
      │         ▼
      │   fraud-alerts (Kafka)
      │         │
      │         ▼
      │   (notification module — ileride)
```

## Outbox Pattern

`TRANSACTION_COMPLETED` ve `TRANSACTION_FAILED` eventleri doğrudan Kafka'ya yazılmaz.
`TransactionService.transfer()` ile aynı DB transaction içinde `outbox_events` tablosuna yazılır.
`OutboxPublisher` (her 500ms) PENDING eventleri okur ve Kafka'ya iletir.
Bu sayede DB ve Kafka arasında atomiklik garantisi sağlanır.
