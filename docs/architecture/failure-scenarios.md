# FinFlow Failure Scenarios

## 1. PostgreSQL Çökerse
- **Etki:** Tüm işlemler durur, API 500 döner
- **Koruma:** Resilience4j retry 3 kez dener
- **Kurtarma:** DB restart, connection pool yenilenir
- **Veri kaybı:** Yok — commit edilmiş veriler güvende

## 2. Redis Çökerse
- **Etki:** Idempotency kontrolü çalışmaz, rate limiting devre dışı
- **Koruma:** Circuit breaker açılır, Redis olmadan da transfer çalışır (degraded mode)
- **Risk:** Duplicate transfer olabilir (idempotency devre dışı)
- **Kurtarma:** Redis restart, cache boş başlar ama sistem çalışır

## 3. Kafka Çökerse
- **Etki:** Event'ler Kafka'ya gönderilemez, fraud detection durur
- **Koruma:** Outbox Pattern sayesinde event'ler DB'de güvenli kalır
- **Kurtarma:** Kafka restart olunca OutboxPublisher event'leri otomatik gönderir
- **Veri kaybı:** Yok — outbox tablosunda PENDING olarak bekler

## 4. Duplicate Request (Aynı istek 2 kez gelirse)
- **Koruma:** Idempotency key Redis'te kontrol edilir
- **Sonuç:** İkinci istek önceki sonucu döner, tekrar işlem yapılmaz
- **Risk:** Redis çökmüşse duplicate olabilir (Senaryo 2)

## 5. Kafka Consumer Başarısız Olursa
- **Koruma:** 3 kez retry, sonra Dead Letter Queue'ya at
- **DLQ:** Başarısız mesajlar `transaction-events-dlq` topic'inde bekler
- **Kurtarma:** Manuel inceleme, DLQ'dan tekrar işleme

## 6. Outbox Publisher Gecikmesi
- **Etki:** Event'ler geç gönderilir, fraud detection gecikir
- **Normal gecikme:** 500ms (poller aralığı)
- **Risk:** Poller durmuşsa event'ler birikir
- **Kurtarma:** Uygulama restart, birikmiş event'ler otomatik gönderilir

## 7. Concurrent Transfer (Aynı anda iki transfer)
- **Koruma:** Optimistic locking (`@Version`), ikinci istek 409 Conflict alır
- **Sonuç:** Para 2 kez gitmez, ikinci kullanıcı tekrar dener

## 8. JWT Token Süresi Dolmuş
- **Etki:** 401 Unauthorized
- **Çözüm:** Yeni token al (`POST /auth/login`)
- **Token süresi:** 24 saat
