package com.example.UserService.security.service;

import com.example.UserService.repository.CardRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CardSecurityService {

    private final CardRepository cardRepository;

    public boolean isOwner(Long cardId, Long userId) {
        return cardRepository.findById(cardId)
                .map(card -> card.getUser().getId().equals(userId))
                .orElse(false);
    }
}

