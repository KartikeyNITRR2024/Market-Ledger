package com.checkin.ingestion.user;
import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponseDto createUser(UserCreateRequestDto requestDto);

    UserResponseDto getUserById(UUID id);

    List<UserResponseDto> getAllUsers();

    UserResponseDto updateUser(UUID id, UserUpdateRequestDto requestDto);

    UserResponseDto changeRole(UUID id, Role role);

    UserResponseDto changeStatus(UUID id, boolean enabled);

    void deleteUser(UUID id);
}