package com.example.UserService.service.card;

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
import com.example.UserService.service.CardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class CardServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private CardRepository cardRepository;
    @Mock
    private CardMapper cardMapper;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;
    @InjectMocks
    private CardService cardService;

    @Test
    void create_shouldCreateCardAndReturnResponse_whenUserExistsAndHasLessThanFiveCards() {
        CardRequest request = new CardRequest();
        request.setUserId(1L);

        User user = new User();
        user.setId(1L);
        user.setCards(new ArrayList<>());

        Card card = new Card();
        Card savedCard = new Card();
        savedCard.setId(10L);

        CardResponse response = new CardResponse();
        response.setId(10L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cardMapper.toEntity(request)).thenReturn(card);
        when(cardRepository.save(card)).thenReturn(savedCard);
        when(cardMapper.toResponse(savedCard)).thenReturn(response);

        CardResponse result = cardService.create(request);

        assertThat(result).isEqualTo(response);

        verify(userRepository).findById(1L);
        verify(cardMapper).toEntity(request);
        verify(cardRepository).save(card);
        verify(cardMapper).toResponse(savedCard);
    }

    @Test
    void create_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        CardRequest request = new CardRequest();
        request.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> cardService.create(request));

        verify(userRepository).findById(1L);
        verifyNoInteractions(cardMapper);
        verify(cardRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowMoreThanFiveCardsException_whenUserAlreadyHasFiveCards() {
        CardRequest request = new CardRequest();
        request.setUserId(1L);

        User user = new User();
        user.setId(1L);
        user.setCards(new ArrayList<>(List.of(
                new Card(), new Card(), new Card(), new Card(), new Card()
        )));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(MoreThanFiveCadsException.class,
                () -> cardService.create(request));

        verify(userRepository).findById(1L);
        verifyNoInteractions(cardMapper);
        verify(cardRepository, never()).save(any());
    }

    @Test
    void getCardById_shouldReturnResponse_whenCardExists() {
        Long id = 1L;

        Card card = new Card();
        card.setId(id);

        CardResponse response = new CardResponse();
        response.setId(id);

        when(cardRepository.findById(id)).thenReturn(Optional.of(card));
        when(cardMapper.toResponse(card)).thenReturn(response);

        CardResponse result = cardService.getCardById(id);

        assertThat(result).isEqualTo(response);

        verify(cardRepository).findById(id);
        verify(cardMapper).toResponse(card);
    }

    @Test
    void getCardById_shouldThrowCardNotFoundException_whenCardDoesNotExist() {
        Long id = 1L;

        when(cardRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class,
                () -> cardService.getCardById(id));

        verify(cardRepository).findById(id);
        verifyNoInteractions(cardMapper);
    }

    @Test
    void findAll_shouldReturnPageOfCards() {
        int page = 0;
        int size = 10;
        String name = "Name";
        String surname = "Surname";

        Pageable pageable = PageRequest.of(page, size);

        Card card = new Card();
        card.setId(1L);

        CardResponse response = new CardResponse();
        response.setId(1L);

        Page<Card> cardPage = new PageImpl<>(List.of(card), pageable, 1);

        when(cardRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(cardPage);

        when(cardMapper.toResponse(card)).thenReturn(response);

        Page<CardResponse> result = cardService.findAll(page, size, name, surname);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(cardRepository).findAll(any(Specification.class), eq(pageable));
        verify(cardMapper).toResponse(card);
    }

    @Test
    void findAllByUserId_shouldReturnListOfCardResponses_whenCardsExist() {
        Long userId = 1L;

        Card card1 = new Card();
        card1.setId(10L);

        Card card2 = new Card();
        card2.setId(20L);

        CardResponse response1 = new CardResponse();
        response1.setId(10L);

        CardResponse response2 = new CardResponse();
        response2.setId(20L);

        when(cardRepository.findAllByUserId(userId))
                .thenReturn(List.of(card1, card2));

        when(cardMapper.toResponse(card1)).thenReturn(response1);
        when(cardMapper.toResponse(card2)).thenReturn(response2);

        List<CardResponse> result = cardService.findAllByUserId(userId);

        assertThat(result).containsExactly(response1, response2);

        verify(cardRepository).findAllByUserId(userId);
        verify(cardMapper).toResponse(card1);
        verify(cardMapper).toResponse(card2);
    }

    @Test
    void findAllByUserId_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        Long id = 1L;

        when(cardRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class,
                () -> cardService.getCardById(id));

        verify(cardRepository).findById(id);
        verifyNoInteractions(cardMapper);
    }

    @Test
    void update_shouldUpdateCardAndReturnResponse_whenCardExists() {
        Long id = 1L;

        CardUpdateRequest request = new CardUpdateRequest();
        request.setHolder("New Holder");

        Card card = new Card();
        card.setId(id);
        card.setHolder("Old Holder");

        User user = new User();
        user.setId(10L);
        card.setUser(user);

        CardResponse response = new CardResponse();
        response.setId(id);
        response.setHolder("New Holder");

        Cache cache = mock(Cache.class);
        when(cacheManager.getCache("users")).thenReturn(cache);

        when(cardRepository.findById(id)).thenReturn(Optional.of(card));
        doNothing().when(cardMapper).updateCardFromRequest(request, card);
        when(cardRepository.save(card)).thenReturn(card);
        when(cardMapper.toResponse(card)).thenReturn(response);

        CardResponse result = cardService.update(request, id);

        assertThat(result).isEqualTo(response);

        verify(cardRepository).findById(id);
        verify(cardMapper).updateCardFromRequest(request, card);
        verify(cardRepository).save(card);
        verify(cacheManager).getCache("users");
        verify(cache).evict(user.getId());
        verify(cardMapper).toResponse(card);
    }

    @Test
    void update_shouldThrowCardNotFoundException_whenCardDoesNotExist() {
        Long id = 1L;
        CardUpdateRequest request = new CardUpdateRequest();

        when(cardRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class,
                () -> cardService.update(request, id));

        verify(cardRepository).findById(id);
        verifyNoInteractions(cardMapper);
        verify(cardRepository, never()).save(any());
    }

    @Test
    void activate_shouldSwitchActive_whenCardIsExists() {
        Long id = 1L;

        Card card = new Card();
        card.setActive(false);

        User user = new User();
        user.setId(100L);
        card.setUser(user);

        CardResponse response = new CardResponse();
        response.setActive(true);

        when(cardRepository.findById(id)).thenReturn(Optional.of(card));
        when(cardMapper.toResponse(card)).thenReturn(response);
        when(cacheManager.getCache("users")).thenReturn(cache);

        CardResponse result = cardService.activate(id);

        assertThat(result).isEqualTo(response);
        assertThat(card.getActive()).isTrue();

        verify(cache).evict(100L);
        verify(cardRepository).findById(id);
        verify(cardRepository).save(card);
        verify(cardMapper).toResponse(card);
    }

    @Test
    void activate_shouldThrowCardNotFoundException_whenCardDoesNotExist() {
        Long id = 1L;

        when(cardRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class,
                () -> cardService.activate(id));

        verify(cardRepository).findById(id);
    }

    @Test
    void deactivate_shouldSwitchActive_whenCardIsExists() {
        Long id = 1L;

        Card card = new Card();
        card.setActive(true);

        User user = new User();
        user.setId(100L);
        card.setUser(user);

        CardResponse response = new CardResponse();
        response.setActive(false);


        when(cardRepository.findById(id)).thenReturn(Optional.of(card));
        when(cardMapper.toResponse(card)).thenReturn(response);
        when(cacheManager.getCache("users")).thenReturn(cache);

        CardResponse result = cardService.deactivate(id);

        assertThat(result).isEqualTo(response);
        assertThat(card.getActive()).isFalse();

        verify(cache).evict(100L);
        verify(cardRepository).findById(id);
        verify(cardRepository).save(card);
        verify(cardMapper).toResponse(card);
    }

    @Test
    void deactivate_shouldThrowCardNotFoundException_whenCardDoesNotExist() {
        Long id = 1L;

        when(cardRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class,
                () -> cardService.deactivate(id));

        verify(cardRepository).findById(id);
    }

    @Test
    void delete_shouldDeleteCard_whenCardExists() {
        Long id = 1L;

        Card card = new Card();

        User user = new User();
        user.setId(100L);
        card.setUser(user);

        when(cardRepository.findById(id)).thenReturn(Optional.of(card));
        when(cacheManager.getCache("users")).thenReturn(cache);

        cardService.delete(id);

        verify(cardRepository).findById(id);
        verify(cardRepository).deleteById(id);
        verify(cache).evict(100L);
    }

    @Test
    void delete_shouldThrowCardNotFoundException_whenCardDoesNotExist() {
        Long id = 1L;

        when(cardRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class,
                () -> cardService.delete(id));

        verify(cardRepository).findById(id);
    }
}
