package com.safehome.backend.domain.property;

import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import com.safehome.backend.common.exception.PropertyNotFoundException;
import com.safehome.backend.common.exception.UserNotFoundException;
import com.safehome.backend.domain.user.User;
import com.safehome.backend.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PropertyOwnerService {

    private final PropertyOwnerRepository propertyOwnerRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public PropertyOwnerService(
            PropertyOwnerRepository propertyOwnerRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository
    ) {
        this.propertyOwnerRepository = propertyOwnerRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    public List<PropertyOwner> findAll() {
        return propertyOwnerRepository.findAll();
    }

    public PropertyOwner findById(Long id) {
        return propertyOwnerRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)
                );
    }

    public List<PropertyOwner> findByPropertyId(Long propertyId) {
        return propertyOwnerRepository.findByPropertyId(propertyId);
    }

    public List<PropertyOwner> findByUserId(Long userId) {
        return propertyOwnerRepository.findByUserId(userId);
    }

    @Transactional
    public PropertyOwner create(
            Long propertyId,
            Long userId,
            BigDecimal ownershipRatio
    ) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException(propertyId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        validateOwnershipRatio(ownershipRatio);

        if (propertyOwnerRepository
                .existsByPropertyIdAndUserId(propertyId, userId)) {

            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        PropertyOwner propertyOwner =
                PropertyOwner.create(property, user, ownershipRatio);

        return propertyOwnerRepository.save(propertyOwner);
    }

    @Transactional
    public PropertyOwner updateOwnershipRatio(
            Long id,
            BigDecimal ownershipRatio
    ) {
        validateOwnershipRatio(ownershipRatio);

        PropertyOwner propertyOwner = findById(id);

        propertyOwner.updateOwnershipRatio(ownershipRatio);

        return propertyOwner;
    }

    @Transactional
    public void verify(Long id) {
        PropertyOwner propertyOwner = findById(id);

        propertyOwner.verify();
    }

    @Transactional
    public void delete(Long id) {
        PropertyOwner propertyOwner = findById(id);

        propertyOwnerRepository.delete(propertyOwner);
    }

    private void validateOwnershipRatio(BigDecimal ownershipRatio) {
        if (ownershipRatio == null) {
            return;
        }

        if (ownershipRatio.compareTo(BigDecimal.ZERO) <= 0
                || ownershipRatio.compareTo(BigDecimal.valueOf(100)) > 0) {

            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}