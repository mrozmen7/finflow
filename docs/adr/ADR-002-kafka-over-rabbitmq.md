# ADR-002: Mesajlaşma Sistemi Olarak Kafka

## Durum
Kabul Edildi

## Tarih
2026-03-25

## Bağlam
Transaction event'lerinin (TRANSACTION_COMPLETED, TRANSACTION_FAILED, FRAUD_ALERT) asenkron
olarak işlenmesi gerekiyor. Finans sektöründe mesajların denetlenebilir ve tekrar oynatılabilir
olması zorunludur.

## Karar
Apache Kafka kullanıyoruz. Mevcut topic'ler: `transaction-events`, `fraud-alerts`,
`transaction-events-dlq`.

## Gerekçe
- **Mesaj kalıcılığı:** Kafka mesajları siler değil saklar — finans audit için kritik.
- **Consumer replay:** Fraud servisinin geçmiş event'leri yeniden işleyebilmesi mümkün.
- **Yüksek throughput:** Finans altyapısı için kanıtlanmış kapasite.
- **Sektör standardı:** İsviçre ve Avrupa finans ekosisteminde yaygın kullanım.

## Reddedilen Alternatifler
| Alternatif | Red Sebebi |
|---|---|
| RabbitMQ | Mesaj replay desteği yok, consumed mesajlar silinir |
| AWS SQS | Vendor lock-in, yerel geliştirme için ek karmaşıklık |
