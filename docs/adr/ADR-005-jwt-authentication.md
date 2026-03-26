# ADR-005: JWT ile Stateless Authentication

## Durum
Kabul Edildi

## Tarih
2026-03-25

## Bağlam
Tüm `/api/**` endpoint'leri kimlik doğrulama gerektiriyor. Sistemin stateless kalması ve
ileride birden fazla servis/instance'a genişleyebilmesi önemli.

## Karar
JWT Bearer token kullanıyoruz. Token `POST /api/v1/auth/login` ile alınıyor, 24 saat
geçerli, `Authorization: Bearer <token>` header'ı ile iletiliyor. Doğrulama `JwtAuthenticationFilter`
tarafından her istekte yapılıyor.

## Gerekçe
- **Stateless:** Her doğrulama için DB çağrısı gerekmez — token kendi bilgisini taşır.
- **Ölçeklenebilir:** Tüm instance'lar aynı secret ile token'ı doğrulayabilir.
- **Microservice-ready:** İleride ayrı servisler token'ı bağımsız doğrulayabilir.
- **Sektör standardı:** OAuth 2.0 / OIDC altyapısına geçişe hazır yapı.

## Reddedilen Alternatifler
| Alternatif | Red Sebebi |
|---|---|
| Session-based auth | Stateful, horizontal scale için sticky session veya paylaşımlı session store gerekir |
| API Key | Kullanıcı bazlı yetkilendirme zor, token yenileme mekanizması yok |
