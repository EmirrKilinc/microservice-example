var builder = WebApplication.CreateBuilder(args);

// Sistemin dışa açık TEK giriş noktası. Route ve cluster tanımları koda değil
// yapılandırmaya (appsettings.json > ReverseProxy) aittir; yeni bir servis eklemek
// yeniden derleme gerektirmez.
//
// Kimlik doğrulama, rate limiting, CORS gibi kesişen konular (cross-cutting concerns)
// servislere tek tek değil buraya eklenir.
builder.Services
    .AddReverseProxy()
    .LoadFromConfig(builder.Configuration.GetSection("ReverseProxy"));

var app = builder.Build();

app.MapGet("/health", () => Results.Ok(new { status = "ok" }));
app.MapReverseProxy();

app.Run();
