using AccountService.Domain.Entities;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;

namespace AccountService.Infrastructure.Persistence;

// Yalnızca geliştirme ortamı için: şemayı oluşturur ve iki örnek hesap ekler.
// Gerçek ortamda şema EF Core migration'ları ile, deploy hattında yönetilir.
public static class DbInitializer
{
    public static readonly Guid SampleAccountA = Guid.Parse("11111111-1111-1111-1111-111111111111");
    public static readonly Guid SampleAccountB = Guid.Parse("22222222-2222-2222-2222-222222222222");

    public static async Task InitializeDatabaseAsync(this IServiceProvider services)
    {
        await using var scope = services.CreateAsyncScope();
        var db = scope.ServiceProvider.GetRequiredService<AccountDbContext>();

        await db.Database.EnsureCreatedAsync();

        if (await db.Accounts.AnyAsync())
            return;

        db.Accounts.AddRange(
            Account.Open(SampleAccountA, "Ada Lovelace", "TR000000000000000000000001", "TRY", 10_000m),
            Account.Open(SampleAccountB, "Alan Turing", "TR000000000000000000000002", "TRY", 2_500m));
        await db.SaveChangesAsync();
    }
}
