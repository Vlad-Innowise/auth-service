package by.innowise.auth.repository;

import by.innowise.auth.repository.entity.AuthUser;
import by.innowise.auth.repository.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AuthUser, Long> {

    @Query("SELECT u FROM AuthUser u WHERE u.email = :email AND u.status = :status")
    Optional<AuthUser> findByEmailAndStatus(@Param("email") String email, UserStatus status);

    @Query("SELECT u FROM AuthUser u WHERE u.id = :id AND u.status = :status")
    Optional<AuthUser> findByIdAndStatus(Long id, UserStatus status);
}
