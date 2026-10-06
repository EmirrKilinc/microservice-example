using AccountService.Application.Accounts;
using Banking.Contracts.Grpc;
using Grpc.Core;

namespace AccountService.API.Grpc;

// gRPC giriş adaptörü. İş mantığı içermez: protobuf mesajını Application komutuna çevirir,
// handler'ı çağırır, sonucu tekrar protobuf'a çevirir. REST endpoint'leri ile aynı
// handler'ları paylaşır.
public sealed class AccountGrpcService(GetBalanceHandler getBalance, DebitAccountHandler debitAccount)
    : AccountGrpc.AccountGrpcBase
{
    public override async Task<GetBalanceReply> GetBalance(GetBalanceRequest request, ServerCallContext context)
    {
        var accountId = ParseAccountId(request.AccountId);

        var balance = await getBalance.HandleAsync(accountId, context.CancellationToken)
            ?? throw new RpcException(new Status(StatusCode.NotFound, "Account not found."));

        return new GetBalanceReply
        {
            AccountId = balance.AccountId.ToString(),
            BalanceMinor = ToMinor(balance.Balance),
            Currency = balance.Currency
        };
    }

    public override async Task<DebitBalanceReply> DebitBalance(DebitBalanceRequest request, ServerCallContext context)
    {
        var accountId = ParseAccountId(request.AccountId);
        if (request.AmountMinor <= 0)
            throw new RpcException(new Status(StatusCode.InvalidArgument, "amount_minor must be positive."));

        var result = await debitAccount.HandleAsync(
            new DebitAccountCommand(accountId, FromMinor(request.AmountMinor), request.Currency, request.IdempotencyKey),
            context.CancellationToken);

        // İş kuralı reddi (yetersiz bakiye vb.) exception değil, yanıtın parçasıdır.
        return new DebitBalanceReply
        {
            Success = result.Success,
            FailureReason = result.Failure switch
            {
                DebitFailure.AccountNotFound => DebitFailureReason.AccountNotFound,
                DebitFailure.InsufficientFunds => DebitFailureReason.InsufficientFunds,
                DebitFailure.CurrencyMismatch => DebitFailureReason.CurrencyMismatch,
                _ => DebitFailureReason.Unspecified
            },
            RemainingBalanceMinor = ToMinor(result.RemainingBalance)
        };
    }

    private static Guid ParseAccountId(string value)
        => Guid.TryParse(value, out var id)
            ? id
            : throw new RpcException(new Status(StatusCode.InvalidArgument, "account_id must be a GUID."));

    // Tel üzerinde kuruş (int64), domain'de decimal. Dönüşüm yalnızca bu sınırda yapılır.
    private static long ToMinor(decimal amount) => (long)decimal.Round(amount * 100m);
    private static decimal FromMinor(long minor) => minor / 100m;
}
