using AccountService.Application.Accounts;

namespace AccountService.API.Endpoints;

// Dışa açık REST yüzeyi. Bilinçli olarak yalnızca OKUMA sunar: bakiye düşme dış dünyaya
// açık değildir, o işlem sadece iç gRPC kontratı üzerinden yapılabilir.
public static class AccountEndpoints
{
    public static IEndpointRouteBuilder MapAccountEndpoints(this IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/api/accounts");

        group.MapGet("/{id:guid}/balance", async (Guid id, GetBalanceHandler handler, CancellationToken ct) =>
        {
            var balance = await handler.HandleAsync(id, ct);
            return balance is null ? Results.NotFound() : Results.Ok(balance);
        });

        return app;
    }
}
