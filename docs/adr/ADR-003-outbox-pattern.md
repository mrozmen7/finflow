# ADR-003: Outbox Pattern ile Event Publishing

## Durum
Kabul Edildi

## Tarih
2026-03-25

## Bağlam
Transfer işlemi tamamlandığında hem veritabanı güncellenip hem de Kafka'ya event gönderilmesi
gerekiyor. İki sistemin aynı anda atomik olarak güncellenmesi (dual-write) mümkün değil —
biri başarılı diğeri başarısız olabilir ve veri tutarsızlığına yol açar.

## Karar
Outbox Pattern kullanıyoruz. `outbox_events` tablosuna event, domain değişikliğiyle aynı
DB transaction içinde yazılıyor. `OutboxPublisher` (500ms interval) bu tabloyu poll ederek
PENDING event'leri Kafka'ya gönderiyor ve SENT olarak işaretliyor.

## Gerekçe
- **Atomiklik:** Event kaydı ve domain değişikliği tek transaction — kısmi başarı imkânsız.
- **Dayanıklılık:** Kafka geçici olarak erişilemez olsa bile event `outbox_events` tablosunda güvende kalır.
- **At-least-once delivery:** Publisher yeniden başladığında PENDING event'leri tekrar gönderir.

## Reddedilen Alternatifler
| Alternatif | Red Sebebi |
|---|---|
| Direct Kafka Publish | Dual-write riski: DB başarılı, Kafka başarısız olursa event kaybolur |
| CDC / Debezium | Ek altyapı (Kafka Connect), bu proje ölçeği için over-engineering |
