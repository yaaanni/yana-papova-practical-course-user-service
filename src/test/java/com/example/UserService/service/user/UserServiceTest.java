package com.example.UserService.service.user;

import com.example.UserService.dto.user.UserByIdResponse;
import com.example.UserService.dto.user.UserRequest;
import com.example.UserService.dto.user.UserResponse;
import com.example.UserService.entities.User;
import com.example.UserService.exception.UserNotFoundException;
import com.example.UserService.exception.UserWithEmailNotFoundException;
import com.example.UserService.mapper.UserMapper;
import com.example.UserService.repository.UserRepository;
import com.example.UserService.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @InjectMocks
    private UserService userService;

    @Test
    void create_shouldSaveUser() {
        UserRequest request = new UserRequest();
        request.setName("Name");
        request.setSurname("Surname");

        User user = new User();
        user.setName("Name");
        user.setSurname("Surname");

        User savedUser = new User();
        savedUser.setName("Name");
        savedUser.setSurname("Surname");

        UserResponse response = new UserResponse();
        response.setName("Name");
        response.setSurname("Surname");

        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(response);

        UserResponse result = userService.create(request);

        assertThat(result).isEqualTo(response);

        verify(userMapper).toEntity(request);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(savedUser);
    }

    @Test
    void getUserById_shouldReturnUser_whenUserIsExists() {
        Long id = 1L;

        User user = new User();
        user.setName("Name");
        user.setSurname("Surname");

        UserByIdResponse response = new UserByIdResponse();
        response.setName("Name");
        response.setSurname("Surname");

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userMapper.toByIdResponse(user)).thenReturn(response);

        UserByIdResponse result = userService.getUserById(id);

        assertThat(result).isEqualTo(response);

        verify(userRepository).findById(id);
        verify(userMapper).toByIdResponse(user);
    }

    @Test
    void getUserByEmail_shouldReturnUser_whenUserExists() {
        String email = "test@mail.com";

        User user = new User();
        user.setName("Name");
        user.setSurname("Surname");

        UserByIdResponse response = new UserByIdResponse();
        response.setName("Name");
        response.setSurname("Surname");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userMapper.toByIdResponse(user)).thenReturn(response);

        UserByIdResponse result = userService.getUserByEmail(email);

        assertThat(result).isEqualTo(response);

        verify(userRepository).findByEmail(email);
        verify(userMapper).toByIdResponse(user);
    }

    @Test
    void getUserById_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        Long id = 1L;

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.getUserById(id));

        verify(userRepository).findById(id);
        verifyNoInteractions(userMapper);
    }

    @Test
    void getUserByEmail_shouldThrowUserWithEmailNotFoundException_whenUserDoesNotExist() {
        String email = "test@mail.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UserWithEmailNotFoundException.class,
                () -> userService.getUserByEmail(email));

        verify(userRepository).findByEmail(email);
        verifyNoInteractions(userMapper);
    }

    @Test
    void findAll_shouldReturnPageOfUsers() {
        int page = 0;
        int size = 10;
        String name = "Name";
        String surname = "Surname";

        Pageable pageable = PageRequest.of(page, size);

        User user = new User();
        user.setName(name);
        user.setSurname(surname);

        UserResponse response = new UserResponse();
        response.setName(name);
        response.setSurname(surname);

        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);

        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(userPage);

        when(userMapper.toResponse(user)).thenReturn(response);

        Page<UserResponse> result = userService.findAll(page, size, name, surname);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(userRepository).findAll(any(Specification.class), eq(pageable));
        verify(userMapper).toResponse(user);
    }

    @Test
    void update_shouldUpdateUser_whenUserIsExists() {
        Long id = 1L;

        UserRequest request = new UserRequest();
        request.setName("NewName");
        request.setSurname("NewSurname");

        User user = new User();
        user.setName("Name");
        user.setSurname("Surname");

        UserResponse response = new UserResponse();
        response.setName("NewName");
        response.setSurname("NewSurname");

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.update(request, id);

        assertThat(result).isEqualTo(response);

        verify(userRepository).findById(id);
        verify(userMapper).updateUserFromRequest(request, user);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    void update_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        Long id = 1L;

        UserRequest request = new UserRequest();
        request.setName("NewName");
        request.setSurname("NewSurname");

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.update(request, id));

        verify(userRepository).findById(id);
        verifyNoInteractions(userMapper);
    }

    @Test
    void activate_shouldSwitchActive_whenUserIsExists() {
        Long id = 1L;

        User user = new User();
        user.setActive(false);

        UserResponse response = new UserResponse();
        response.setActive(true);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.activate(id);

        assertThat(result).isEqualTo(response);

        verify(userRepository).findById(id);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    void activate_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        Long id = 1L;

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.activate(id));

        verify(userRepository).findById(id);
    }

    @Test
    void deactivate_shouldSwitchActive_whenUserIsExists() {
        Long id = 1L;

        User user = new User();
        user.setActive(true);

        UserResponse response = new UserResponse();
        response.setActive(false);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.deactivate(id);

        assertThat(result).isEqualTo(response);

        verify(userRepository).findById(id);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    void deactivate_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        Long id = 1L;

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.deactivate(id));

        verify(userRepository).findById(id);
    }

    @Test
    void delete_shouldDeleteUser_whenUserExists() {
        Long id = 1L;

        User user = new User();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        userService.delete(id);

        verify(userRepository).findById(id);
        verify(userRepository).deleteById(id);
    }

    @Test
    void delete_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        Long id = 1L;

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.delete(id));

        verify(userRepository).findById(id);
    }
}
