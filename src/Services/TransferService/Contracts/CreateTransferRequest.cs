namespace TransferService.Contracts;

// Bu servisin dışa açık HTTP kontratı. Banking.Contracts'a konmaz: orası servisler ARASI
// kontratlar içindir, bu ise yalnızca TransferService'in kendi API'sine aittir.
public sealed record CreateTransferRequest(Guid FromAccountId, Guid ToAccountId, decimal Amount, string Currency);

public sealed record TransferResponse(Guid TransferId, string Status, string? Reason);
