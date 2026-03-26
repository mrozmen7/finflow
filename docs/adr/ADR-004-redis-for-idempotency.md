# ADR-004: Redis ile Idempotency ve Rate Limiting

## Durum
Kabul Edildi

## Tarih
2026-03-25

## Bağlam
Ağ hataları veya client retry'ları nedeniyle aynı transfer isteği birden fazla kez gelebilir.
Ayrıca tek bir hesabın dakikada aşırı sayıda transfer başlatmasının engellenmesi gerekiyor.

## Karar
Redis kullanıyoruz.
- **Idempotency:** `idempotency:{key}` → transaction UUID, 24 saat TTL.
- **Rate limiting:** `rate-limit:{accountId}` → istek sayacı, 1 dakika TTL, limit 10.
- **Fraud idempotency:** `fraud-processed:{transactionId}` → 48 saat TTL.

## Gerekçe
- **Hız:** Sub-millisecond okuma — hot path'te gecikme eklemez.
- **TTL:** Otomatik süre sonu, manuel temizlik gerektirmez.
- **Dağıtık:** Birden fazla uygulama instance'ı aynı Redis'i paylaşabilir.
- **Spring Boot entegrasyonu:** `spring-boot-starter-data-redis` ile minimal konfigürasyon.

## Reddedilen Alternatifler
| Alternatif | Red Sebebi |
|---|---|
| DB tabanlı idempotency | Her transfer için ekstra SELECT/INSERT — yavaş, DB yükü artar |
| In-memory (ConcurrentHashMap) | Uygulama restart'ta kaybolur, birden fazla instance'da çalışmaz |
