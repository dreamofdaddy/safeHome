package com.safehome.backend.common.code;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "code_values",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_code_values_group_code",
                        columnNames = {"group_id", "code"}
                )
        }
)
public class CodeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "code_values_seq")
    @SequenceGenerator(
            name = "code_values_seq",
            sequenceName = "code_values_id_seq",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "group_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_code_values_group")
    )
    private CodeGroup group;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "code_name", nullable = false, length = 100)
    private String codeName;

    @Column(name = "description")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "use_yn",
            nullable = false,
            columnDefinition = "char(1)"
    )
    private String useYn = "Y";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected CodeValue() {
    }

    public CodeValue(
            CodeGroup group,
            String code,
            String codeName,
            String description,
            Integer sortOrder,
            String useYn
    ) {
        this.group = group;
        this.code = code;
        this.codeName = codeName;
        this.description = description;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
        this.useYn = useYn != null ? useYn : "Y";
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (sortOrder == null) {
            sortOrder = 0;
        }

        if (useYn == null) {
            useYn = "Y";
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public CodeGroup getGroup() {
        return group;
    }

    public String getCode() {
        return code;
    }

    public String getCodeName() {
        return codeName;
    }

    public String getDescription() {
        return description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public String getUseYn() {
        return useYn;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            String codeName,
            String description,
            Integer sortOrder,
            String useYn
    ) {
        this.codeName = codeName;
        this.description = description;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
        this.useYn = useYn != null ? useYn : "Y";
    }
}