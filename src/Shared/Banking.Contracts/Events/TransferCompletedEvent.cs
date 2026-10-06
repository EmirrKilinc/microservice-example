namespace Banking.Contracts.Events;

// Integration event: servis sınırını aşan, "olmuş bitmiş" bir olguyu anlatır (geçmiş zaman).
// Yayınlayan (TransferService) kimin dinlediğini bilmez; yeni bir tüketici eklemek
// yayınlayan tarafta kod değişikliği gerektirmez.
//
// MassTransit exchange adını tipin tam adından (namespace:type) türetir. Publisher ve
// consumer'ın aynı tipi bu ortak assembly'den alması bu yüzden zorunludur.
//
// Şema evrimi: alan eklemek güvenlidir; alan silmek/yeniden adlandırmak kırıcıdır.
public sealed record TransferCompletedEvent(
    Guid TransferId,
    Guid FromAccountId,
    Guid ToAccountId,
    decimal Amount,
    string Currency,
    DateTime CompletedAtUtc);
