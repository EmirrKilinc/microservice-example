using AccountService.Application.Accounts;
using Microsoft.Extensions.DependencyInjection;

namespace AccountService.Application;

// Her katman kendi kayıtlarını kendi yapar; API'nin Program.cs'i iç detayları bilmez.
public static class DependencyInjection
{
    public static IServiceCollection AddApplication(this IServiceCollection services)
    {
        services.AddScoped<GetBalanceHandler>();
        services.AddScoped<DebitAccountHandler>();
        return services;
    }
}
