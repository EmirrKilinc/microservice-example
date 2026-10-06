using Banking.Contracts.Grpc;
using MassTransit;
using TransferService.Application;
using TransferService.Endpoints;

var builder = WebApplication.CreateBuilder(args);

// Senkron bağımlılık: Account Service'in gRPC istemcisi. Typed client, HttpClientFactory
// üzerinden yönetilir (bağlantı havuzu, DI, merkezi adres yapılandırması).
// Üretimde buraya retry/circuit breaker politikası eklenir.
builder.Services.AddGrpcClient<AccountGrpc.AccountGrpcClient>(options =>
{
    options.Address = new Uri(
        builder.Configuration["Services:AccountGrpc"]
        ?? throw new InvalidOperationException("Services:AccountGrpc is missing."));
});

// Asenkron bağımlılık: RabbitMQ. Bu servis yalnızca yayınlar, consumer kaydı yoktur.
builder.Services.AddMassTransit(x =>
{
    x.UsingRabbitMq((_, cfg) =>
    {
        cfg.Host(builder.Configuration["RabbitMq:Host"], "/", h =>
        {
            h.Username(builder.Configuration["RabbitMq:Username"] ?? "guest");
            h.Password(builder.Configuration["RabbitMq:Password"] ?? "guest");
        });
    });
});

builder.Services.AddScoped<TransferOrchestrator>();

var app = builder.Build();

app.MapTransferEndpoints();
app.MapGet("/health", () => Results.Ok(new { status = "ok" }));

app.Run();
