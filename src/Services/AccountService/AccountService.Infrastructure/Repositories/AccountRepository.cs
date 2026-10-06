using AccountService.Application.Abstractions;
using AccountService.Domain.Entities;
using AccountService.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace AccountService.Infrastructure.Repositories;

// Adapter: IAccountRepository port'unun EF Core uygulaması.
internal sealed class AccountRepository(AccountDbContext db) : IAccountRepository
{
    public Task<Account?> GetByIdAsync(Guid id, CancellationToken ct)
        => db.Accounts.FirstOrDefaultAsync(a => a.Id == id, ct);
}
