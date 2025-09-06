package by.innowise.auth.mapper;

import by.innowise.auth.dto.UserCreateDto;
import by.innowise.auth.repository.entity.AuthUser;
import by.innowise.auth.repository.entity.UserStatus;
import by.innowise.auth.util.MapperHelper;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = BaseMapper.class,
        uses = MapperHelper.class)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "role", qualifiedByName = "convertToRole")
    AuthUser toEntity(UserCreateDto dto, @Context UserStatus status, @Context String hashedPassword);

    @AfterMapping
    default void finishEntityMapping(@MappingTarget AuthUser user,
                                     @Context UserStatus status,
                                     @Context String hashedPassword) {
        user.setPassword(hashedPassword);
        user.setStatus(status);
    }

}
