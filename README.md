# Banking Microservices — İskelet Proje

Spring Boot 4 (Java 17) üzerinde, mimariyi incelemek için hazırlanmış minimal bir bankacılık mikroservis iskeleti.
Amaç üretime hazır bir sistem değil; servis sınırlarını, katmanları ve iletişim tarzlarını okunabilir halde göstermek.

> **Durum:** Proje .NET 10 sürümünden Spring Boot'a çevrildi (eski sürüm git geçmişinde).
> `./mvnw package` derleniyor; REST, gRPC ve Gateway yolu yerelde denendi.
> PostgreSQL, RabbitMQ ve `docker compose` ile uçtan uca çalıştırma henüz denenmedi.

## Dizin yapısı

```
.
├── pom.xml                          # parent: ortak derleme ayarları + merkezi sürümler
├── mvnw, mvnw.cmd                   # Maven wrapper (Maven kurulumu gerekmez)
├── docker-compose.yml
├── api-gateway/                     # Spring Cloud Gateway — dışa açık tek giriş noktası
│   └── src/main/resources/application.yml   # route tanımları
├── services
│   ├── account-service/             # tek modül — N katmanlı mimari
│   │   └── com.banking.account
│   │       ├── controller/          # sunum: GET /api/accounts/{id}/balance
│   │       ├── grpc/                # sunum: AccountGrpcService (iç servisler için)
│   │       ├── service/             # iş mantığı: AccountService
│   │       ├── repository/          # veri erişimi: Spring Data JPA + PostgreSQL
│   │       ├── entity/              # Account (@Entity)
│   │       ├── dto/                 # katmanlar arası taşınan veri
│   │       └── config/              # dev verisi
│   ├── transfer-service/            # tek modül — orkestrasyon
│   │   └── com.banking.transfer
│   │       ├── web/                 # POST /api/transfers
│   │       ├── application/         # TransferOrchestrator (gRPC çağrısı + event publish)
│   │       ├── contracts/           # servisin kendi HTTP DTO'ları
│   │       └── config/              # gRPC istemcisi, RabbitMQ
│   └── notification-service/        # worker — HTTP yok
│       └── consumers/               # @RabbitListener
└── shared
    └── banking-contracts/           # servisler arası kontratlar
        ├── src/main/java/.../events/TransferCompletedEvent.java
        └── src/main/protobuf/account.proto
```

## İstek akışı

```
İstemci
  │  POST /api/transfers
  ▼
api-gateway (Spring Cloud Gateway, :5000)
  │  HTTP/1.1  →  transfer-service:8082
  ▼
transfer-service ──── gRPC (HTTP/2, senkron) ────►  account-service :8081 ──► account-db (PostgreSQL)
  │                   DebitBalance                    AccountGrpcService
  │                                                     → AccountService.debit() → AccountRepository
  │  convertAndSend(TransferCompletedEvent)
  ▼
RabbitMQ ──── asenkron ────►  notification-service
                               TransferCompletedConsumer → konsola log
```

`GET /api/accounts/{id}/balance` isteği ise Gateway üzerinden doğrudan account-service'in REST portuna (8080) gider.

## Portlar

| Servis | Port | Ne için |
|---|---|---|
| api-gateway | 5000 | Dışa açık tek port |
| account-service | 8080 | REST (Tomcat) |
| account-service | 8081 | gRPC (Netty) |
| transfer-service | 8082 | REST |
| notification-service | — | Port açmaz |

## Mimari kararlar

| Karar | Neden |
|---|---|
| Bakiye düşme **gRPC ile senkron** | Sonuç hemen gerekli: bakiye yetmiyorsa transfer başlamamalı. |
| Bildirim **RabbitMQ ile asenkron** | Yan etki transferi bekletmemeli; notification-service kapalıyken de transfer tamamlanır. |
| account-service'te **iki port** (8080 REST, 8081 gRPC) | Dış (north-south) ve iç (east-west) trafik ayrışır. |
| Bakiye düşme **REST'te yok** | Yalnızca iç gRPC kontratından yapılabilir; Gateway üzerinden dışarıya açılmaz. |
| **Database-per-service** | `account-db`'ye yalnızca account-service bağlanır; diğerleri kontrat üzerinden erişir. |
| `banking-contracts`'ta **yalnızca kontrat** | Paylaşılan iş mantığı servisleri birbirine kilitler. |
| `account.proto` **tek kopya** (shared'da) | account-service server sınıfını, transfer-service client stub'ını aynı dosyadan üretir. |
| account-service **N katmanlı** (controller → service → repository → entity) | Küçük bir servis için yeterli ve okunması kolay; katmanlar paket, modül değil. |
| İş kuralları **`AccountService`'te** | REST ve gRPC aynı service'i çağırır; `Account` yalnızca veri taşıyan bir `@Entity`'dir. |
| transfer-service'te **repository/entity yok** | Kendi verisi yok, işi orkestrasyon. |
| gRPC'de para **int64 kuruş** | Protobuf'ta decimal yok; kayan nokta kullanılmaz. |
| Event exchange'i **fanout**, adı kontratta | Her tüketici kendi kuyruğunu bağlar; yayınlayan kimin dinlediğini bilmez. |

## Bilinçli olarak eksik bırakılanlar

Kodda ilgili yerlerde yorum olarak işaretlendi:

- **Alıcı hesaba yatırma ve telafi (Saga):** Transfer şu an yalnızca gönderen hesaptan düşüyor.
- **Transactional Outbox:** Bakiye düştükten sonra publish başarısız olursa event kaybolur.
- **Idempotency:** `idempotency_key` taşınıyor ama sunucuda kontrol edilmiyor.
- **Kimlik doğrulama / yetkilendirme**, retry / circuit breaker, dead-letter kuyruğu, OpenTelemetry, şema migration'ları (Flyway/Liquibase), testler.

## Çalıştırma

```bash
docker compose up --build
```

Örnek hesaplar (`dev` profilinde otomatik eklenir):

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
Sağlık kontrolü: `/actuator/health` (gateway, account-service, transfer-service).

### Docker dışında

```bash
./mvnw package                       # tüm modüller (JDK 17+)
java -jar services/account-service/target/account-service.jar --spring.profiles.active=dev
java -jar services/transfer-service/target/transfer-service.jar
java -jar services/notification-service/target/notification-service.jar
java -jar api-gateway/target/api-gateway.jar
```

`application.yml` dosyalarındaki varsayılan adresler `localhost`'tur; PostgreSQL (5432) ve RabbitMQ (5672)
yerelde erişilebilir olmalıdır. Compose bu portları host'a yayınlamaz.
