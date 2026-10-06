# Banking Microservices — İskelet Proje

.NET 10 üzerinde, mimariyi incelemek için hazırlanmış minimal bir bankacılık mikroservis iskeleti.
Amaç üretime hazır bir sistem değil; servis sınırlarını, katmanları ve iletişim tarzlarını okunabilir halde göstermek.

> **Durum:** Bu iskelet derlenmeden yazıldı (makinede .NET SDK yoktu). `Directory.Packages.props`
> içindeki paket sürümleri doğrulanmadı; ilk `dotnet restore` sırasında düzeltme gerekebilir.

## Dizin yapısı

```
.
├── Banking.slnx
├── Directory.Build.props            # ortak derleme ayarları (net10.0, nullable)
├── Directory.Packages.props         # merkezi paket sürümleri
├── docker-compose.yml
└── src
    ├── ApiGateway/                  # YARP — dışa açık tek giriş noktası
    │   ├── Program.cs
    │   └── appsettings.json         # route + cluster tanımları
    ├── Services
    │   ├── AccountService/          # Clean Architecture, 4 proje
    │   │   ├── AccountService.Domain/          # Account aggregate, domain exception'ları
    │   │   ├── AccountService.Application/     # use case handler'ları, port arayüzleri
    │   │   ├── AccountService.Infrastructure/  # AccountDbContext, repository (EF Core + PostgreSQL)
    │   │   └── AccountService.API/             # REST endpoint'leri + AccountGrpcService
    │   ├── TransferService/         # tek proje — orkestrasyon
    │   │   ├── Endpoints/           # POST /api/transfers
    │   │   ├── Application/         # TransferOrchestrator (gRPC çağrısı + event publish)
    │   │   └── Contracts/           # servisin kendi HTTP DTO'ları
    │   └── NotificationService/     # worker — HTTP yok
    │       └── Consumers/           # IConsumer<TransferCompletedEvent>
    └── Shared
        └── Banking.Contracts/       # servisler arası kontratlar
            ├── Events/TransferCompletedEvent.cs
            └── Protos/account.proto
```

## İstek akışı

```
İstemci
  │  POST /api/transfers
  ▼
ApiGateway (YARP, :5000)
  │  HTTP/1.1  →  transfer-service:8080
  ▼
TransferService ──── gRPC (HTTP/2, senkron) ────►  AccountService :8081 ──► account-db (PostgreSQL)
  │                  DebitBalance                    AccountGrpcService
  │                                                    → DebitAccountHandler → Account.Debit()
  │  Publish(TransferCompletedEvent)
  ▼
RabbitMQ ──── asenkron ────►  NotificationService
                               TransferCompletedConsumer → konsola log
```

`GET /api/accounts/{id}/balance` isteği ise Gateway üzerinden doğrudan AccountService'in REST portuna (8080) gider.

## Mimari kararlar

| Karar | Neden |
|---|---|
| Bakiye düşme **gRPC ile senkron** | Sonuç hemen gerekli: bakiye yetmiyorsa transfer başlamamalı. |
| Bildirim **RabbitMQ ile asenkron** | Yan etki transferi bekletmemeli; NotificationService kapalıyken de transfer tamamlanır. |
| AccountService'te **iki port** (8080 REST, 8081 gRPC) | TLS'siz gRPC, HTTP/2'yi ayrı portta ister. Ayrıca dış ve iç trafik ayrışır. |
| Bakiye düşme **REST'te yok** | Yalnızca iç gRPC kontratından yapılabilir; Gateway üzerinden dışarıya açılmaz. |
| **Database-per-service** | `account-db`'ye yalnızca AccountService bağlanır; diğerleri kontrat üzerinden erişir. |
| `Banking.Contracts`'ta **yalnızca kontrat** | Paylaşılan iş mantığı servisleri birbirine kilitler. |
| `account.proto` **tek kopya** (Shared'da) | AccountService server stub'ını, TransferService client stub'ını aynı dosyadan üretir. |
| TransferService **tek proje** | Zengin domain'i yok; her servise dört katman dayatmak gereksiz. |
| gRPC'de para **int64 kuruş** | Protobuf'ta decimal yok; kayan nokta kullanılmaz. |
| **MediatR yok** | İki use case için düz handler sınıfları yeterli. |

## Bilinçli olarak eksik bırakılanlar

Kodda ilgili yerlerde yorum olarak işaretlendi:

- **Alıcı hesaba yatırma ve telafi (Saga):** Transfer şu an yalnızca gönderen hesaptan düşüyor.
- **Transactional Outbox:** Bakiye düştükten sonra `Publish` başarısız olursa event kaybolur.
- **Idempotency:** `idempotency_key` taşınıyor ama sunucuda kontrol edilmiyor.
- **Kimlik doğrulama / yetkilendirme**, retry / circuit breaker, OpenTelemetry, EF Core migration'ları, testler.

## Çalıştırma

```bash
docker compose up --build
```

Örnek hesaplar (Development ortamında otomatik eklenir):

| Hesap | Id | Bakiye |
|---|---|---|
| A | `11111111-1111-1111-1111-111111111111` | 10 000 TRY |
| B | `22222222-2222-2222-2222-222222222222` | 2 500 TRY |

```bash
curl http://localhost:5000/api/accounts/11111111-1111-1111-1111-111111111111/balance

curl -X POST http://localhost:5000/api/transfers \
  -H "Content-Type: application/json" \
  -d '{"fromAccountId":"11111111-1111-1111-1111-111111111111","toAccountId":"22222222-2222-2222-2222-222222222222","amount":250,"currency":"TRY"}'

docker compose logs -f notification-service   # [NOTIFICATION] satırı
```

RabbitMQ yönetim arayüzü: http://localhost:15672 (guest / guest).

Docker dışında çalıştırırken Gateway'in `appsettings.json` dosyasındaki hedef adresler
(`account-service`, `transfer-service`) compose ağındaki DNS adlarıdır; yerelde `localhost` ile değiştirilmelidir.
