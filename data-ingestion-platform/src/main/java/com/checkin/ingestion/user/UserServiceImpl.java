package com.checkin.ingestion.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.checkin.ingestion.common.exceptions.ResourceNotFoundException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponseDto createUser(UserCreateRequestDto requestDto) {
        log.info("Initiating registration for user with email: '{}' and assigned role: {}", 
                requestDto.getEmail(), requestDto.getRole());

        if (userRepository.existsByEmail(requestDto.getEmail())) {
            log.warn("Registration rejected: email '{}' already in use", requestDto.getEmail());
            throw new IllegalArgumentException("User with email '" + requestDto.getEmail() + "' already exists");
        }

        User user = User.builder()
                .email(requestDto.getEmail())
                .passwordHash(passwordEncoder.encode(requestDto.getPassword()))
                .role(requestDto.getRole())
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully. Assigned ID: {}, Role: {}", savedUser.getId(), savedUser.getRole());

        return mapToResponseDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(UUID id) {
        log.debug("Fetching user details for ID: {}", id);

        User user = findUserOrThrow(id);
        log.debug("User found: ID={}, Email={}, Role={}, Enabled={}", 
                user.getId(), user.getEmail(), user.getRole(), user.isEnabled());

        return mapToResponseDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        log.info("Fetching all registered users from database");

        List<User> users = userRepository.findAll();
        log.info("Total users retrieved: {}", users.size());

        return users.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(UUID id, UserUpdateRequestDto requestDto) {
        log.info("Updating profile details for user ID: {}", id);

        User user = findUserOrThrow(id);

        if (!user.getEmail().equalsIgnoreCase(requestDto.getEmail()) && userRepository.existsByEmail(requestDto.getEmail())) {
            log.warn("Update rejected for user ID: {}. Target email '{}' is already registered", id, requestDto.getEmail());
            throw new IllegalArgumentException("Email '" + requestDto.getEmail() + "' is already in use");
        }

        String previousEmail = user.getEmail();
        user.setEmail(requestDto.getEmail());
        User updatedUser = userRepository.save(user);

        log.info("User ID: {} email updated from '{}' to '{}'", id, previousEmail, updatedUser.getEmail());
        return mapToResponseDto(updatedUser);
    }

    @Override
    @Transactional
    public UserResponseDto changeRole(UUID id, Role newRole) {
        log.info("Processing role modification request for user ID: {} -> New Role: {}", id, newRole);

        User user = findUserOrThrow(id);
        Role oldRole = user.getRole();

        if (oldRole == newRole) {
            log.info("User ID: {} already possesses role {}. No update applied", id, newRole);
            return mapToResponseDto(user);
        }

        user.setRole(newRole);
        User updatedUser = userRepository.save(user);

        log.info("Role change applied for user ID: {}. Changed from {} to {}", id, oldRole, updatedUser.getRole());
        return mapToResponseDto(updatedUser);
    }

    @Override
    @Transactional
    public UserResponseDto changeStatus(UUID id, boolean enabled) {
        log.info("Updating active status for user ID: {} to enabled={}", id, enabled);

        User user = findUserOrThrow(id);

        if (user.isEnabled() == enabled) {
            log.info("User ID: {} already has enabled status set to {}. Skipping update", id, enabled);
            return mapToResponseDto(user);
        }

        user.setEnabled(enabled);
        User updatedUser = userRepository.save(user);

        log.warn("User status updated: User ID: {} is now {}", id, enabled ? "ENABLED" : "DISABLED");
        return mapToResponseDto(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        log.warn("Request received to permanently delete user with ID: {}", id);

        User user = findUserOrThrow(id);
        userRepository.delete(user);

        log.info("User ID: {} and email '{}' permanently removed", id, user.getEmail());
    }

    private User findUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Operation failed: user with ID {} was not found", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });
    }

    private UserResponseDto mapToResponseDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
