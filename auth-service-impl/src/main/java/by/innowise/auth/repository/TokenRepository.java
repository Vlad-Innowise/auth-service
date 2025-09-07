package by.innowise.auth.repository;

import by.innowise.auth.repository.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findTokenByTokenHash(String tokenHash);
}
