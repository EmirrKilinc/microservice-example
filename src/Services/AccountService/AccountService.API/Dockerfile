# Build context repo köküdür (docker-compose.yml'e bakın): servis, src/Shared altındaki
# kontrat projesine ve kökteki Directory.*.props dosyalarına ihtiyaç duyar.
FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /repo
COPY Directory.Build.props Directory.Packages.props ./
COPY src/Shared/ src/Shared/
COPY src/Services/AccountService/ src/Services/AccountService/
RUN dotnet publish src/Services/AccountService/AccountService.API/AccountService.API.csproj -c Release -o /app

FROM mcr.microsoft.com/dotnet/aspnet:10.0
WORKDIR /app
COPY --from=build /app .
EXPOSE 8080 8081
ENTRYPOINT ["dotnet", "AccountService.API.dll"]
