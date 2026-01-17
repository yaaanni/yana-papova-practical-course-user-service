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
import com.example.UserService.specification.CardSpecifications;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@AllArgsConstructor
public class CardService {
    private final CardMapper cardMapper;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    public CardResponse create(CardRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new UserNotFoundException(request.getUserId()));
        if (user.getCards().size() >= 5) {
            throw new MoreThanFiveCadsException(request.getUserId());
        }
        Card card = cardMapper.toEntity(request);
        card.setUser(user);
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
        return cardMapper.toResponse(card);
    }

    @Transactional
    public CardResponse active(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(id));
        cardRepository.activate(id);
        return cardMapper.toResponse(card);
    }

    @Transactional
    public CardResponse deactive(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(id));
        cardRepository.deactivate(id);
        return cardMapper.toResponse(card);
    }

    @Transactional
    public void delete(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(id));
        cardRepository.deleteById(id);
    }
}
