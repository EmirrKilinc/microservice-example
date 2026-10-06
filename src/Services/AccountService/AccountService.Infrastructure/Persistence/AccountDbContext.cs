using AccountService.Application.Abstractions;
using AccountService.Domain.Entities;
using Microsoft.EntityFrameworkCore;

namespace AccountService.Infrastructure.Persistence;

// Database-per-service: bu DbContext yalnızca Account Service'e aittir ve kendi
// veritabanına (account-db) bağlanır. Başka hiçbir servis bu tablolara doğrudan
// erişemez; hesap verisine ihtiyaç duyan servis gRPC/REST kontratından geçmek zorundadır.
public sealed class AccountDbContext(DbContextOptions<AccountDbContext> options)
    : DbContext(options), IUnitOfWork
{
    public DbSet<Account> Accounts => Set<Account>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
        => modelBuilder.ApplyConfigurationsFromAssembly(typeof(AccountDbContext).Assembly);

    async Task IUnitOfWork.SaveChangesAsync(CancellationToken ct) => await SaveChangesAsync(ct);
}
