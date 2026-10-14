package com.safehome.backend.domain.property;

import com.safehome.backend.domain.property.dto.PropertyOwnerCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PropertyOwnerRegistrationIntegrationTest {
    @Autowired private PropertyOwnerService ownerService;
    @Autowired private PropertyOwnerRepository ownerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MockMvc mockMvc;

    @Test
    void registryNumberIdentifiesExistingProperty() {
        assertThat(propertyRepository.findByRegistryPropertyNumber("1413-1996-141271"))
                .get().extracting(Property::getId).isEqualTo(5L);
    }

    @Test
    void storesOwnerUnderSnapshotWithoutExposingIdentifier() throws Exception {
        var request = new PropertyOwnerCreateRequest(5L, 2L, "Second Owner",
                "760101-*******", new BigDecimal("30.00"), null);
        var owner = ownerService.create(request);
        assertThat(owner.getRegistrySnapshot().getId()).isEqualTo(2L);
        assertThat(ownerRepository.findByRegistrySnapshotId(2L)).hasSize(2);

        byte[] ciphertext = jdbcTemplate.queryForObject(
                "SELECT owner_rrn_encrypted FROM property_owners WHERE id = ?",
                byte[].class, owner.getId());
        assertThat(ciphertext).isNotEqualTo("760101-*******".getBytes());
        assertThat(owner.getOwnerRrnBirth6Hash()).hasSize(64);

        mockMvc.perform(post("/api/property-owners")
                        .contentType("application/json")
                        .content("""
                                {"propertyId":5,"registrySnapshotId":2,"ownerName":"Third Owner",
                                 "ownerRrnMasked":"810101-*******","ownershipRatio":20.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ownerName").value("Third Owner"))
                .andExpect(jsonPath("$.data.ownerRrnMasked").doesNotExist())
                .andExpect(jsonPath("$.data.ownerRrnBirth6Hash").doesNotExist());
    }
}
