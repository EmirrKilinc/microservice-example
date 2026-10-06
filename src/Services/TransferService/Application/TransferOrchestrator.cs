using Banking.Contracts.Events;
using Banking.Contracts.Grpc;
using MassTransit;
using TransferService.Contracts;

namespace TransferService.Application;

public sealed record TransferResult(Guid TransferId, bool Completed, string? Reason);

// Transfer akışının orkestrasyonu. İki iletişim tarzı bilinçli olarak ayrılmıştır:
//
//   1) Bakiye düşme  -> gRPC, SENKRON. Sonuca hemen ihtiyaç var: bakiye yetmiyorsa
//      transfer hiç başlamamalı ve kullanıcıya anında yanıt dönmeli.
//   2) "Transfer tamamlandı" -> RabbitMQ, ASENKRON. Bildirim gibi yan etkiler transferi
//      bekletmemeli; NotificationService kapalı olsa bile transfer başarılı sayılır.
public sealed class TransferOrchestrator(
    AccountGrpc.AccountGrpcClient accounts,
    IPublishEndpoint publisher,
    ILogger<TransferOrchestrator> logger)
{
    public async Task<TransferResult> ExecuteAsync(CreateTransferRequest request, CancellationToken ct)
    {
        var transferId = NewId.NextGuid();

        // Deadline: Account Service yanıt vermezse bu istek sonsuza kadar asılı kalmaz;
        // bir servisteki yavaşlık zincirleme şekilde diğerlerine yayılmaz.
        var debit = await accounts.DebitBalanceAsync(
            new DebitBalanceRequest
            {
                AccountId = request.FromAccountId.ToString(),
                AmountMinor = (long)decimal.Round(request.Amount * 100m),
                Currency = request.Currency,
                IdempotencyKey = transferId.ToString()
            },
            deadline: DateTime.UtcNow.AddSeconds(5),
            cancellationToken: ct);

        if (!debit.Success)
        {
            logger.LogInformation("Transfer {TransferId} rejected: {Reason}", transferId, debit.FailureReason);
            return new TransferResult(transferId, false, debit.FailureReason.ToString());
        }

        // İskelette eksik bırakılan iki konu:
        //  - Alıcı hesaba yatırma (credit) ve o adım başarısız olursa düşümü geri alma
        //    (compensation). Dağıtık transaction yoktur; bu bir Saga ile çözülür.
        //  - Dual write: bakiye düştü ama aşağıdaki Publish başarısız olursa event kaybolur.
        //    Üretimde Transactional Outbox kullanılır (MassTransit'in EF Core outbox'ı).
        await publisher.Publish(
            new TransferCompletedEvent(
                transferId,
                request.FromAccountId,
                request.ToAccountId,
                request.Amount,
                request.Currency,
                DateTime.UtcNow),
            ct);

        return new TransferResult(transferId, true, null);
    }
}
