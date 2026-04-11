package com.example.UserService.repository;

import com.example.UserService.entities.Card;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    Card save(Card card);

    Optional<Card> findById(Long id);

    Page<Card> findAll(Specification<Card> spec, Pageable pageable);

    @Query("SELECT c FROM Card c WHERE c.user.id = :userId")
    List<Card> findAllByUserId(@Param("userId") Long id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE payment_cards SET active = true WHERE id = :id", nativeQuery = true)
    void activate(@Param("id") Long id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE payment_cards SET active = false WHERE id = :id", nativeQuery = true)
    void deactivate(@Param("id") Long id);

    void deleteById(Long id);
}
