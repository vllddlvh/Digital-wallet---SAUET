package sa.uet.digitalwallet_system.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import sa.uet.digitalwallet_system.adapter.out.persistence.entity.TransactionJpaEntity;
import sa.uet.digitalwallet_system.domain.model.Transaction;

@Mapper(componentModel = "spring")
public interface TransactionEntityMapper {
    Transaction toDomain(TransactionJpaEntity entity);
    TransactionJpaEntity toEntity(Transaction domain);
}
