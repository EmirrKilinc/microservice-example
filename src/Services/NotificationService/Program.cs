using MassTransit;
using NotificationService.Consumers;

// Generic Host (web host değil): hiçbir HTTP endpoint'i açılmaz.
// MassTransit kendi hosted service'ini kaydeder ve kuyruğu dinlemeye başlar.
var builder = Host.CreateApplicationBuilder(args);

builder.Services.AddMassTransit(x =>
{
    x.AddConsumer<TransferCompletedConsumer>();

    x.UsingRabbitMq((context, cfg) =>
    {
        cfg.Host(builder.Configuration["RabbitMq:Host"], "/", h =>
        {
            h.Username(builder.Configuration["RabbitMq:Username"] ?? "guest");
            h.Password(builder.Configuration["RabbitMq:Password"] ?? "guest");
        });

        // Her consumer için kuyruğu ve exchange binding'ini otomatik oluşturur.
        // Kuyruk bu servise aittir: servisin birden çok kopyası aynı kuyruğu paylaşır
        // (competing consumers), yani her event yalnızca bir kopya tarafından işlenir.
        cfg.ConfigureEndpoints(context);
    });
});

builder.Build().Run();
