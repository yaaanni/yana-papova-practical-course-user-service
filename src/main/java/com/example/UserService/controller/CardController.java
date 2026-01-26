package com.example.UserService.controller;

import com.example.UserService.dto.card.CardRequest;
import com.example.UserService.dto.card.CardResponse;
import com.example.UserService.dto.card.CardUpdateRequest;
import com.example.UserService.service.CardService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cards")
@AllArgsConstructor
public class CardController {
    private final CardService cardService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CardResponse> create(@RequestBody @Valid CardRequest request) {
        CardResponse response = cardService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN') or @cardSecurityService.isOwner(#id, principal.getUserId())")
    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getCardById(@PathVariable Long id) {
        CardResponse response = cardService.getCardById(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<CardResponse>> getCards(@RequestParam int page, @RequestParam int size, @RequestParam(required = false) String name, @RequestParam(required = false) String surname) {
        Page<CardResponse> cards = cardService.findAll(page, size, name, surname);
        return ResponseEntity.ok(cards);
    }

    @PreAuthorize("hasRole('ADMIN') or @cardSecurityService.isOwner(#id, principal.getUserId())")
    @GetMapping("/{id}/all")
    public ResponseEntity<List<CardResponse>> getCardsById(@PathVariable Long id) {
        List<CardResponse> cards = cardService.findAllByUserId(id);
        return ResponseEntity.ok(cards);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<CardResponse> update(@RequestBody CardUpdateRequest request, @PathVariable Long id) {
        CardResponse response = cardService.update(request, id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<CardResponse> activate(@PathVariable Long id) {
        CardResponse response = cardService.activate(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<CardResponse> deactivate(@PathVariable Long id) {
        CardResponse response = cardService.deactivate(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cardService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
