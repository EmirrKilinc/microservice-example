using AccountService.Domain.Entities;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;

namespace AccountService.Infrastructure.Persistence.Configurations;

// Eşleme bilgisi (tablo, kolon, index) Domain'de attribute olarak değil burada durur;
// entity sınıfı persistence detayından habersiz kalır.
internal sealed class AccountConfiguration : IEntityTypeConfiguration<Account>
{
    public void Configure(EntityTypeBuilder<Account> builder)
    {
        builder.ToTable("accounts");
        builder.HasKey(a => a.Id);

        builder.Property(a => a.OwnerName).HasMaxLength(200).IsRequired();
        builder.Property(a => a.Iban).HasMaxLength(34).IsRequired();
        builder.HasIndex(a => a.Iban).IsUnique();
        builder.Property(a => a.Currency).HasMaxLength(3).IsRequired();
        builder.Property(a => a.Balance).HasPrecision(18, 2);

        // PostgreSQL'de uint + IsRowVersion, sistem kolonu xmin'e eşlenir:
        // UPDATE ... WHERE xmin = @eski değer. Eşleşmezse DbUpdateConcurrencyException.
        builder.Property(a => a.Version).IsRowVersion();
    }
}
