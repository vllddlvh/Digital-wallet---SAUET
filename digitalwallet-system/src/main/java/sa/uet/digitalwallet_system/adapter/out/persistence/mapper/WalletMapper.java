package sa.uet.digitalwallet_system.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import sa.uet.digitalwallet_system.adapter.out.persistence.entity.WalletJpaEntity;
import sa.uet.digitalwallet_system.domain.model.Wallet;

@Mapper(componentModel = "spring")
public interface WalletMapper {
    Wallet toDomain(WalletJpaEntity entity);
    WalletJpaEntity toEntity(Wallet domain);
}
