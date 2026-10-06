namespace AccountService.Domain.Exceptions;

// Domain kuralı ihlalleri. Application katmanı bunları yakalayıp
// dış dünyaya uygun bir sonuca (HTTP 422, gRPC failure_reason) çevirir.
public abstract class DomainException(string message) : Exception(message);

public sealed class InsufficientFundsException(Guid accountId, decimal balance, decimal requested)
    : DomainException($"Account {accountId} has {balance}, requested {requested}.");

public sealed class CurrencyMismatchException(Guid accountId, string expected, string actual)
    : DomainException($"Account {accountId} is in {expected}, request was in {actual}.");
