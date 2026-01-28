package com.example.UserService.repository;

import com.example.UserService.entities.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    User save(User user);

    @EntityGraph(attributePaths = "cards")
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findById(@Param("id") Long id);

    Optional<User> findByEmail(String email);

    Page<User> findAll(Specification<User> spec, Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET active = true WHERE id = :id", nativeQuery = true)
    void activate(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET active = false WHERE id = :id", nativeQuery = true)
    void deactivate(@Param("id") Long id);

    void deleteById(Long id);
}
