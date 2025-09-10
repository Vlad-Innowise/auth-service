package by.innowise.auth.util;

import by.innowise.internship.security.dto.Role;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
public class MapperHelper {

    @Named("convertToRole")
    public Role convertToRole(String rawRole) {
        return Role.valueOf(rawRole);
    }
}
