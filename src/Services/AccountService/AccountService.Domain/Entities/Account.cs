using AccountService.Domain.Exceptions;

namespace AccountService.Domain.Entities;

// Aggregate root. İş kuralları (invariant'lar) burada yaşar; setter'lar private olduğu için
// bakiye yalnızca Debit/Credit üzerinden, kurallar kontrol edilerek değişebilir.
public sealed class Account
{
    public Guid Id { get; private set; }
    public string OwnerName { get; private set; } = string.Empty;
    public string Iban { get; private set; } = string.Empty;
    public string Currency { get; private set; } = string.Empty;
    public decimal Balance { get; private set; }

    // Optimistic concurrency token'ı. Aynı hesaba eşzamanlı iki düşüm gelirse
    // ikincisi eski sürümle yazmaya çalışır ve reddedilir (kayıp güncelleme olmaz).
    public uint Version { get; private set; }

    // EF Core için.
    private Account() { }

    public static Account Open(Guid id, string ownerName, string iban, string currency, decimal initialBalance)
    {
        if (initialBalance < 0)
            throw new ArgumentOutOfRangeException(nameof(initialBalance));

        return new Account
        {
            Id = id,
            OwnerName = ownerName,
            Iban = iban,
            Currency = currency,
            Balance = initialBalance
        };
    }

    public void Debit(decimal amount, string currency)
    {
        if (amount <= 0)
            throw new ArgumentOutOfRangeException(nameof(amount));
        if (!string.Equals(currency, Currency, StringComparison.OrdinalIgnoreCase))
            throw new CurrencyMismatchException(Id, Currency, currency);
        if (Balance < amount)
            throw new InsufficientFundsException(Id, Balance, amount);

        Balance -= amount;
    }

    public void Credit(decimal amount, string currency)
    {
        if (amount <= 0)
            throw new ArgumentOutOfRangeException(nameof(amount));
        if (!string.Equals(currency, Currency, StringComparison.OrdinalIgnoreCase))
            throw new CurrencyMismatchException(Id, Currency, currency);

        Balance += amount;
    }
}
