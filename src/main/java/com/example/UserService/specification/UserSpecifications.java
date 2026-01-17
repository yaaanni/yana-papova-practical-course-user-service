package com.example.UserService.specification;

import com.example.UserService.entities.User;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecifications {
    private UserSpecifications() {
    }

    public static Specification<User> hasName(String name) {
        return (root, query, criteriaBuilder) ->
                name == null ? null : criteriaBuilder.equal(root.get("name"), name);
    }

    public static Specification<User> hasSurname(String surname) {
        return (root, query, criteriaBuilder) ->
                surname == null ? null : criteriaBuilder.equal(root.get("surname"), surname);
    }
}
