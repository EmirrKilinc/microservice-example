using Grpc.Core;
using TransferService.Application;
using TransferService.Contracts;

namespace TransferService.Endpoints;

public static class TransferEndpoints
{
    public static IEndpointRouteBuilder MapTransferEndpoints(this IEndpointRouteBuilder app)
    {
        app.MapPost("/api/transfers", async (
            CreateTransferRequest request,
            TransferOrchestrator orchestrator,
            CancellationToken ct) =>
        {
            if (request.Amount <= 0 || request.FromAccountId == request.ToAccountId)
                return Results.BadRequest(new { error = "Amount must be positive and accounts must differ." });

            try
            {
                var result = await orchestrator.ExecuteAsync(request, ct);

                return result.Completed
                    ? Results.Created($"/api/transfers/{result.TransferId}",
                        new TransferResponse(result.TransferId, "Completed", null))
                    // İstek geçerli ama iş kuralı reddetti (ör. yetersiz bakiye).
                    : Results.UnprocessableEntity(
                        new TransferResponse(result.TransferId, "Rejected", result.Reason));
            }
            catch (RpcException ex) when (ex.StatusCode is StatusCode.Unavailable or StatusCode.DeadlineExceeded)
            {
                // Alt servis hatası istemciye iç detay sızdırmadan 503 olarak çevrilir.
                return Results.Problem("Account service is unavailable.", statusCode: StatusCodes.Status503ServiceUnavailable);
            }
        });

        return app;
    }
}
