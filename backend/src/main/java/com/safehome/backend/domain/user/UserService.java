package com.safehome.backend.domain.user;

import com.safehome.backend.common.code.CodeService;
import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import com.safehome.backend.common.exception.UserNotFoundException;
import com.safehome.backend.domain.user.dto.UserCreateRequest;
import com.safehome.backend.domain.user.dto.UserResponse;
import com.safehome.backend.domain.user.dto.UserUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final CodeService codeService;
    private final UserRepository userRepository;

    public UserService(
            UserRepository userRepository,
            CodeService codeService
    ) {
        this.userRepository = userRepository;
        this.codeService = codeService;
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        validateVerificationStatus(request.getVerificationStatus());
        validateStatus(request.getStatus());

        User user = new User(
                request.getEmail(),
                request.getName(),
                request.getPhone(),
                request.getVerificationStatus(),
                request.getStatus()
        );

        User saved = userRepository.save(user);

        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        validateVerificationStatus(request.getVerificationStatus());
        validateStatus(request.getStatus());

        user.update(
                request.getEmail(),
                request.getName(),
                request.getPhone(),
                request.getVerificationStatus(),
                request.getStatus()
        );

        return UserResponse.from(user);
    }

    @Transactional
    public void delete(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        userRepository.delete(user);
    }

    private void validateVerificationStatus(String verificationStatus) {
        if (verificationStatus == null || verificationStatus.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        codeService.getRequiredActiveCode(
                "USER_VERIFICATION",
                verificationStatus
        );
    }

    private void validateStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        codeService.getRequiredActiveCode(
                "USER_STATUS",
                status
        );
    }
}