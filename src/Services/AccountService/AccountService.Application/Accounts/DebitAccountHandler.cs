using AccountService.Application.Abstractions;
using AccountService.Domain.Exceptions;

namespace AccountService.Application.Accounts;

public sealed record DebitAccountCommand(Guid AccountId, decimal Amount, string Currency, string IdempotencyKey);

public enum DebitFailure
{
    None,
    AccountNotFound,
    InsufficientFunds,
    CurrencyMismatch
}

public sealed record DebitResult(bool Success, DebitFailure Failure, decimal RemainingBalance);

// Use case: bakiye düşme. Kuralın kendisi Account.Debit içindedir; handler yalnızca
// akışı yönetir (yükle -> domain metodunu çağır -> kaydet) ve sonucu çevirir.
public sealed class DebitAccountHandler(IAccountRepository accounts, IUnitOfWork unitOfWork)
{
    public async Task<DebitResult> HandleAsync(DebitAccountCommand command, CancellationToken ct)
    {
        // İskelette eksik bırakıldı: command.IdempotencyKey daha önce işlendiyse aynı sonucu
        // tekrar dönmek gerekir (işlenmiş anahtarlar tablosu). Aksi halde istemcinin
        // timeout sonrası retry'ı parayı iki kez düşer.
        var account = await accounts.GetByIdAsync(command.AccountId, ct);
        if (account is null)
            return new DebitResult(false, DebitFailure.AccountNotFound, 0);

        try
        {
            account.Debit(command.Amount, command.Currency);
        }
        catch (InsufficientFundsException)
        {
            return new DebitResult(false, DebitFailure.InsufficientFunds, account.Balance);
        }
        catch (CurrencyMismatchException)
        {
            return new DebitResult(false, DebitFailure.CurrencyMismatch, account.Balance);
        }

        await unitOfWork.SaveChangesAsync(ct);
        return new DebitResult(true, DebitFailure.None, account.Balance);
    }
}
