using AccountService.Application.Abstractions;
using AccountService.Infrastructure.Persistence;
using AccountService.Infrastructure.Repositories;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;

namespace AccountService.Infrastructure;

public static class DependencyInjection
{
    public static IServiceCollection AddInfrastructure(this IServiceCollection services, string connectionString)
    {
        services.AddDbContext<AccountDbContext>(options => options.UseNpgsql(connectionString));

        services.AddScoped<IAccountRepository, AccountRepository>();
        // Repository ile aynı scope'taki DbContext örneği: tek istek = tek transaction sınırı.
        services.AddScoped<IUnitOfWork>(sp => sp.GetRequiredService<AccountDbContext>());

        return services;
    }
}
