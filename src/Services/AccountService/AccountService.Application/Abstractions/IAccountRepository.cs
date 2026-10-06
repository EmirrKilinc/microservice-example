using AccountService.Domain.Entities;

namespace AccountService.Application.Abstractions;

// Port: Application ihtiyacını tanımlar, Infrastructure uygular (Dependency Inversion).
// Bu sayede use case'ler veritabanı olmadan, sahte bir repository ile test edilebilir.
public interface IAccountRepository
{
    Task<Account?> GetByIdAsync(Guid id, CancellationToken ct);
}

public interface IUnitOfWork
{
    Task SaveChangesAsync(CancellationToken ct);
}
