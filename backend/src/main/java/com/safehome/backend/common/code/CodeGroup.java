package com.safehome.backend.common.code;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "code_groups",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_code_groups_group_code",
                        columnNames = "group_code"
                )
        }
)
public class CodeGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "code_groups_seq")
    @SequenceGenerator(
            name = "code_groups_seq",
            sequenceName = "code_groups_id_seq",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "group_code", nullable = false, length = 50)
    private String groupCode;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

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

    protected CodeGroup() {
    }

    public CodeGroup(
            String groupCode,
            String groupName,
            String description,
            Integer sortOrder,
            String useYn
    ) {
        this.groupCode = groupCode;
        this.groupName = groupName;
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

    public String getGroupCode() {
        return groupCode;
    }

    public String getGroupName() {
        return groupName;
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
            String groupName,
            String description,
            Integer sortOrder,
            String useYn
    ) {
        this.groupName = groupName;
        this.description = description;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
        this.useYn = useYn != null ? useYn : "Y";
    }
}