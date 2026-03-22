package com.example.UserService.service;

import com.example.UserService.dto.card.CardRequest;
import com.example.UserService.dto.card.CardResponse;
import com.example.UserService.dto.card.CardUpdateRequest;
import com.example.UserService.entities.Card;
import com.example.UserService.entities.User;
import com.example.UserService.exception.CardNotFoundException;
import com.example.UserService.exception.MoreThanFiveCadsException;
import com.example.UserService.exception.UserNotFoundException;
import com.example.UserService.mapper.CardMapper;
import com.example.UserService.repository.CardRepository;
import com.example.UserService.repository.UserRepository;
import com.example.UserService.security.model.AuthUser;
import com.example.UserService.specification.CardSpecifications;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CardService {
    private final CardMapper cardMapper;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    @CacheEvict(value = "users", key = "#request.userId")
    public CardResponse create(CardRequest request, AuthUser authUser) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            request.setUserId(authUser.getUserId());
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new UserNotFoundException(request.getUserId()));
        if (user.getCards().size() >= 5) {
            throw new MoreThanFiveCadsException(request.getUserId());
        }
        Card card = cardMapper.toEntity(request);
        card.setUser(user);
        user.getCards().add(card);
        Card savedCard = cardRepository.save(card);
        return cardMapper.toResponse(savedCard);
    }

    public CardResponse getCardById(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(id));
        return cardMapper.toResponse(card);
    }

    public Page<CardResponse> findAll(int page, int size, String name, String surname) {
        Specification<Card> spec = Specification
                .where(CardSpecifications.hasName(name))
                .and(CardSpecifications.hasSurname(surname));
        PageRequest pageable = PageRequest.of(page, size);
        return cardRepository.findAll(spec, pageable)
                .map(c -> cardMapper.toResponse(c));
    }

    public List<CardResponse> findAllByUserId(Long id) {
        return cardRepository.findAllByUserId(id)
                .stream()
                .map(c -> cardMapper.toResponse(c))
                .toList();
    }

    @Transactional
    public CardResponse update(CardUpdateRequest cardRequest, Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(id));
        cardMapper.updateCardFromRequest(cardRequest, card);
        cardRepository.save(card);
        cacheManager.getCache("users").evict(card.getUser().getId());
        return cardMapper.toResponse(card);
    }

    @Transactional
    public CardResponse activate(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(id));
        card.setActive(true);
        cardRepository.save(card);
        cacheManager.getCache("users").evict(card.getUser().getId());
        return cardMapper.toResponse(card);
    }

    @Transactional
    public CardResponse deactivate(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(id));
        card.setActive(false);
        cardRepository.save(card);
        cacheManager.getCache("users").evict(card.getUser().getId());
        return cardMapper.toResponse(card);
    }

    @Transactional
    public void delete(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(id));
        cacheManager.getCache("users").evict(card.getUser().getId());
        cardRepository.deleteById(id);
    }
}
