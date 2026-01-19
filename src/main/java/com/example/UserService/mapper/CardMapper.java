package com.example.UserService.mapper;

import com.example.UserService.dto.card.CardRequest;
import com.example.UserService.dto.card.CardResponse;
import com.example.UserService.dto.card.CardUpdateRequest;
import com.example.UserService.entities.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CardMapper {
    @Mapping(target = "user", ignore = true)
    Card toEntity(CardRequest request);

    CardResponse toResponse(Card card);

    void updateCardFromRequest(CardUpdateRequest request, @MappingTarget Card card);
}
