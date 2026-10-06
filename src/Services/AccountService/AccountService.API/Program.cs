using AccountService.API.Endpoints;
using AccountService.API.Grpc;
using AccountService.Application;
using AccountService.Infrastructure;
using AccountService.Infrastructure.Persistence;

var builder = WebApplication.CreateBuilder(args);

// Composition root: katmanlar burada birbirine bağlanır.
builder.Services.AddApplication();
builder.Services.AddInfrastructure(
    builder.Configuration.GetConnectionString("AccountDb")
    ?? throw new InvalidOperationException("ConnectionStrings:AccountDb is missing."));

builder.Services.AddGrpc();

var app = builder.Build();

if (app.Environment.IsDevelopment())
    await app.Services.InitializeDatabaseAsync();

// Aynı servis iki giriş kapısı sunar (portlar appsettings.json > Kestrel altında):
//   8080 / HTTP1 -> REST: Gateway üzerinden dış istemciler (north-south trafik)
//   8081 / HTTP2 -> gRPC: iç ağdaki servisler (east-west trafik)
// TLS'siz gRPC, HTTP/2'yi ayrı bir portta "prior knowledge" ile ister; iki port bu yüzden var.
app.MapAccountEndpoints();
app.MapGrpcService<AccountGrpcService>();

app.MapGet("/health", () => Results.Ok(new { status = "ok" }));

app.Run();
