using Banking.Contracts.Events;
using MassTransit;

namespace NotificationService.Consumers;

// TransferService'i tanımaz; yalnızca ortak event kontratını bilir.
// Teslimat "at-least-once"tır: aynı event birden fazla kez gelebilir. Gerçek bir
// SMS/e-posta gönderiminde TransferId ile tekrar kontrolü (idempotent consumer) gerekir.
public sealed class TransferCompletedConsumer(ILogger<TransferCompletedConsumer> logger)
    : IConsumer<TransferCompletedEvent>
{
    public Task Consume(ConsumeContext<TransferCompletedEvent> context)
    {
        var transfer = context.Message;

        // Simülasyon: gerçek bir bildirim sağlayıcısı yerine konsola log.
        logger.LogInformation(
            "[NOTIFICATION] Transfer {TransferId} tamamlandı: {Amount} {Currency}, {From} -> {To} ({CompletedAt:u})",
            transfer.TransferId,
            transfer.Amount,
            transfer.Currency,
            transfer.FromAccountId,
            transfer.ToAccountId,
            transfer.CompletedAtUtc);

        // Exception fırlatılırsa MassTransit mesajı yeniden dener, sonunda _error kuyruğuna taşır.
        return Task.CompletedTask;
    }
}
