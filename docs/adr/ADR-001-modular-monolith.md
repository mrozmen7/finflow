# ADR-001: Modular Monolith Mimarisi

## Durum
Kabul Edildi

## Tarih
2026-03-24

## Bağlam
Tek developer tarafından geliştirilen, finans domain'ine odaklanan bir portfolyo projesi.
Gelecekte ölçeklenebilir bir yapıya ihtiyaç duyulabilir; ancak mevcut aşamada operasyonel
karmaşıklığı minimumda tutmak önceliklidir.

## Karar
Microservice yerine modular monolith kullanıyoruz. Modüller (`transaction`, `fraud`, `shared`)
birbirleriyle doğrudan method çağrısı yerine Kafka event'leri üzerinden haberleşiyor.

## Gerekçe
- Tek developer için microservice altyapısı (service discovery, distributed tracing, deployment pipeline) gereksiz overhead oluşturur.
- Kafka üzerinden haberleşen modüller, ileride bağımsız servisler olarak ayrılabilir — sınırlar şimdiden çizili.
- Finans domain'inde domain logic'in sağlam olması, dağıtık mimari kurmaktan önce gelir.

## Reddedilen Alternatifler
| Alternatif | Red Sebebi |
|---|---|
| Microservices | Tek developer için operasyonel karmaşıklık orantısız |
| Pure Monolith | Modül sınırları belirsiz, ileride ayrıştırmak güç |
