package com.safehome.backend.domain.property;

import com.safehome.backend.common.code.CodeService;
import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import com.safehome.backend.common.exception.PropertyNotFoundException;
import com.safehome.backend.domain.property.dto.PropertyCreateRequest;
import com.safehome.backend.domain.property.dto.PropertyResponse;
import com.safehome.backend.domain.property.dto.PropertyUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PropertyService {

    private final CodeService codeService;
    private final PropertyRepository propertyRepository;

    public PropertyService(
        PropertyRepository propertyRepository,
        CodeService codeService
    ) {
        this.propertyRepository = propertyRepository;
        this.codeService = codeService;
    }

    public List<PropertyResponse> findAll() {
        return propertyRepository.findAll()
            .stream()
            .map(PropertyResponse::from)
            .toList();
    }

    public PropertyResponse findById(Long id) {
        Property property = getProperty(id);
        return PropertyResponse.from(property);
    }

    @Transactional
    public PropertyResponse create(PropertyCreateRequest request) {

        validateBuildingType(request.buildingType());
        
        Property property = new Property(
            request.address(),
            request.roadAddress(),
            request.buildingName(),
            request.buildingType(),
            request.unitNumber(),
            request.postalCode()
        );

        Property saved = propertyRepository.save(property);

        return PropertyResponse.from(saved);
    }

    @Transactional
    public PropertyResponse update(
        Long id,
        PropertyUpdateRequest request
    ) {
        Property property = getProperty(id);

        validateBuildingType(request.buildingType());

        property.update(
            request.address(),
            request.roadAddress(),
            request.buildingName(),
            request.buildingType(),
            request.unitNumber(),
            request.postalCode()
        );

        return PropertyResponse.from(property);
    }

    @Transactional
    public void delete(Long id) {
        Property property = getProperty(id);
        propertyRepository.delete(property);
    }

    private void validateBuildingType(String buildingType) {
        if (buildingType == null || buildingType.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        codeService.getRequiredActiveCode(
            "PROPERTY_TYPE",
            buildingType
        );
    }

    private Property getProperty(Long id) {
        return propertyRepository.findById(id)
            .orElseThrow(() -> new PropertyNotFoundException(id));
    }
}