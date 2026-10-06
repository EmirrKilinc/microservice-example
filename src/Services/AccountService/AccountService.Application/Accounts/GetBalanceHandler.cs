using AccountService.Application.Abstractions;

namespace AccountService.Application.Accounts;

public sealed record BalanceDto(Guid AccountId, decimal Balance, string Currency);

// Use case: bakiye sorgulama. Hem REST endpoint'i hem gRPC servisi bu handler'ı çağırır;
// taşıma protokolü değişse de iş akışı tek yerde kalır.
// Not: MediatR bilinçli olarak kullanılmadı; iki use case için düz sınıflar yeterli.
public sealed class GetBalanceHandler(IAccountRepository accounts)
{
    public async Task<BalanceDto?> HandleAsync(Guid accountId, CancellationToken ct)
    {
        var account = await accounts.GetByIdAsync(accountId, ct);
        return account is null ? null : new BalanceDto(account.Id, account.Balance, account.Currency);
    }
}
