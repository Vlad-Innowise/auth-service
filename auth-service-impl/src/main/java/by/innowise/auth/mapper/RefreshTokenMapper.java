package by.innowise.auth.mapper;

import by.innowise.auth.repository.entity.RefreshToken;
import by.innowise.auth.service.dto.RefreshTokenCreateDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = BaseMapper.class)
public interface RefreshTokenMapper {

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    RefreshToken toEntity(RefreshTokenCreateDto d);
}
