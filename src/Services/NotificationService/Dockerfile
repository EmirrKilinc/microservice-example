# Build context repo köküdür.
FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /repo
COPY Directory.Build.props Directory.Packages.props ./
COPY src/Shared/ src/Shared/
COPY src/Services/NotificationService/ src/Services/NotificationService/
RUN dotnet publish src/Services/NotificationService/NotificationService.csproj -c Release -o /app

# Worker olduğu için ASP.NET Core runtime'ı gerekmez; daha küçük olan runtime imajı yeterli.
FROM mcr.microsoft.com/dotnet/runtime:10.0
WORKDIR /app
COPY --from=build /app .
ENTRYPOINT ["dotnet", "NotificationService.dll"]
