package com.rafdev.prova.blog.api.service.impl;

import com.rafdev.prova.blog.api.dto.UserDto;
import com.rafdev.prova.blog.api.entity.ERole;
import com.rafdev.prova.blog.api.entity.Role;
import com.rafdev.prova.blog.api.entity.User;
import com.rafdev.prova.blog.api.exception.ResourceAlreadyExistsException;
import com.rafdev.prova.blog.api.exception.ResourceNotFoundException;
import com.rafdev.prova.blog.api.repository.RoleRepository;
import com.rafdev.prova.blog.api.repository.UserRepository;
import com.rafdev.prova.blog.api.request.UserRequest;
import com.rafdev.prova.blog.api.service.RoleService;
import com.rafdev.prova.blog.api.service.UserService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserServiceImpl implements UserService {

    private final String resourceName = "User";
    private final AtomicLong idCounter = new AtomicLong(10);
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, RoleService roleService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleService = roleService;
    }

    @Override
    public List<UserDto> getUsers() {
        List<UserDto> usersDto = new ArrayList<>();
        List<User> users = userRepository.findAll();

        for (User user : users) {
            UserDto userDto = new UserDto(user);

            usersDto.add(userDto);
        }

        return usersDto;
    }

    @Override
    public UserDto saveUser(UserRequest userRequest) throws ResourceAlreadyExistsException {

        if (userRepository.existsByEmailIgnoreCase(userRequest.getEmail())) {
            throw new ResourceAlreadyExistsException(resourceName, "Email", userRequest.getEmail());
        }

        if (userRepository.existsByUsernameIgnoreCase(userRequest.getUsername())) {
            throw new ResourceAlreadyExistsException(resourceName, "Username", userRequest.getUsername());
        }

        List<Role> roles = setUserRoles(userRequest.getRoles());

        User user = new User(userRequest.getUsername(), userRequest.getEmail(),
                getPasswordHashed(userRequest.getPassword()), userRequest.getFirstName(), userRequest.getLastName(), roles);

        User userCreated = userRepository.save(user);

        return new UserDto(userCreated);
    }

    @Override
    public UserDto getUserById(Long id) {

        return new UserDto(getUserOrThrowException(id));
    }

    @Override
    public void deleteUserById(Long id) {

        userRepository.delete(getUserOrThrowException(id));
    }

    @Override
    public UserDto updateUserById(Long id, UserRequest userRequest) {
        User userFound = getUserOrThrowException(id);

        List<Role> roles = setUserRoles(userRequest.getRoles());

        if (!Objects.equals(userFound.getUsername(), userRequest.getUsername())) {
            if (userRepository.existsByUsernameIgnoreCase(userRequest.getUsername())) {
                throw new ResourceAlreadyExistsException(resourceName, "Username", userRequest.getUsername());
            }
        }

        if (!Objects.equals(userFound.getEmail(), userRequest.getEmail())) {
            if (userRepository.existsByUsernameIgnoreCase(userRequest.getEmail())) {
                throw new ResourceAlreadyExistsException(resourceName, "Email", userRequest.getEmail());
            }
        }

        userFound.setUsername(userRequest.getUsername());
        userFound.setEmail(userRequest.getEmail());
        userFound.setPassword(getPasswordHashed(userRequest.getPassword()));
        userFound.setFirstName(userRequest.getFirstName());
        userFound.setLastName(userRequest.getLastName());
        userFound.setRoles(roles);

        return new UserDto(userRepository.save(userFound));
    }

    private String getPasswordHashed(String password) {
        return passwordEncoder.encode(password);
    }

    private List<Role> setUserRoles(List<String> strRoles) {
        List<Role> roles = new ArrayList<>();

        if (strRoles == null || strRoles.isEmpty()) {
            roles.add(getRole(ERole.ROLE_USER));
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "super_admin" -> roles.add(getRole(ERole.ROLE_SUPER_ADMIN));

                    case "admin" -> roles.add(getRole(ERole.ROLE_ADMIN));

                    default -> roles.add(getRole(ERole.ROLE_USER));
                }
            });
        }

        return roles;
    }

    private User getUserOrThrowException(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(resourceName, "Id", id));
    }

    private Role getRole(ERole name) {
        return roleService.getOrCreateByName(name);
    }
}
