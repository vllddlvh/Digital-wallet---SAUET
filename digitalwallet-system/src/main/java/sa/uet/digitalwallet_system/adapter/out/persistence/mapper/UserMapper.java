package sa.uet.digitalwallet_system.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sa.uet.digitalwallet_system.adapter.out.persistence.entity.UserJpaEntity;
import sa.uet.digitalwallet_system.domain.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "passwordHash", target = "passwordHash")
    User toDomain(UserJpaEntity entity);

    @Mapping(source = "passwordHash", target = "passwordHash")
    UserJpaEntity toEntity(User domain);
}
