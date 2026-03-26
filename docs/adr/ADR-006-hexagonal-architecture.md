# ADR-006: Hexagonal Architecture (Ports & Adapters)

## Durum
Kabul Edildi

## Tarih
2026-03-24

## Bağlam
Kod organizasyonu ve test edilebilirlik için tutarlı bir mimari pattern seçilmesi gerekiyor.
Domain logic'in framework, veritabanı ve messaging altyapısından bağımsız kalması önemli.

## Karar
Hexagonal Architecture (Ports & Adapters) kullanıyoruz. Her modülde dört katman:
- `domain/` — saf iş mantığı, framework bağımlılığı yok
- `application/` — use case'ler, port tanımları
- `infrastructure/` — DB adapter (JPA), Kafka adapter
- `api/` — REST adapter (Spring MVC)

## Gerekçe
- **Bağımsızlık:** Domain logic'i test etmek için Spring context'e gerek yok — pure unit test.
- **Değiştirilebilirlik:** PostgreSQL yerine başka DB kullanılsa domain katmanı değişmez.
- **Sınır netliği:** Modüller arası bağımlılık yönü açık — `domain` hiçbir şeye bağımlı değil.
- **Test kolaylığı:** Port interface'leri mock'lanarak application servisleri izole test edilebilir.

## Reddedilen Alternatifler
| Alternatif | Red Sebebi |
|---|---|
| Layered Architecture | Katmanlar arası tight coupling yaygın, domain framework'e bağımlı hale gelir |
| Clean Architecture | Bu proje ölçeği için over-engineering; use case sınıfları gereksiz karmaşıklık ekler |
