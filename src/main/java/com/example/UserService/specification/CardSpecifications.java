package com.example.UserService.specification;

import com.example.UserService.entities.Card;
import org.springframework.data.jpa.domain.Specification;

public class CardSpecifications {
    private CardSpecifications() {
    }

    public static Specification<Card> hasName(String name) {
        return (root, query, criteriaBuilder) ->
                name == null ? null : criteriaBuilder.equal(root.join("user").get("name"), name);
    }

    public static Specification<Card> hasSurname(String surname) {
        return (root, query, criteriaBuilder) ->
                surname == null ? null : criteriaBuilder.equal(root.join("user").get("surname"), surname);
    }
}
