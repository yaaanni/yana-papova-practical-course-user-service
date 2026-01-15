package com.example.UserService.service;

import com.example.UserService.dto.user.UserByIdResponse;
import com.example.UserService.dto.user.UserRequest;
import com.example.UserService.dto.user.UserResponse;
import com.example.UserService.entities.User;
import com.example.UserService.exception.MoreThanFiveCadsException;
import com.example.UserService.exception.UserNotFoundException;
import com.example.UserService.mapper.UserMapper;
import com.example.UserService.repository.UserRepository;
import com.example.UserService.specification.UserSpecifications;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;

    public UserResponse create(UserRequest request) {
        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    public UserByIdResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return userMapper.toByIdResponse(user);
    }

    public Page<UserResponse> findAll(int page, int size, String name, String surname) {
        Specification<User> spec = Specification
                .where(UserSpecifications.hasName(name))
                .and(UserSpecifications.hasSurname(surname));
        PageRequest pageable = PageRequest.of(page, size);
        return userRepository.findAll(spec, pageable)
                .map(u -> userMapper.toResponse(u));
    }

    public UserResponse update(UserRequest request, Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        userMapper.updateUserFromRequest(request, user);
        userRepository.save(user);
        return userMapper.toResponse(user);
    }

    public UserResponse activate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        userRepository.activate(id);
        return userMapper.toResponse(user);
    }

    public UserResponse deactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        userRepository.deactivate(id);
        return userMapper.toResponse(user);
    }

    public void delete(Long id) {
        userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        userRepository.deleteById(id);
    }
}
