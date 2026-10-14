package com.safehome.backend.domain.user;

import com.safehome.backend.common.exception.UserNotFoundException;
import com.safehome.backend.domain.user.dto.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        return UserResponse.from(user);
    }
}
