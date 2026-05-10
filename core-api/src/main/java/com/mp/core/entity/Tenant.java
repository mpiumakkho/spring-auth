package com.mp.core.entity;

import java.util.Date;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tenants", schema = "sample_app")
@EntityListeners(AuditingEntityListener.class)
public class Tenant {

    public static final String DEFAULT_TENANT_ID = "default-tenant";

    @Id
    @Column(name = "tenant_id", length = 255)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(nullable = false, length = 20)
    private String status = "active";

    @CreatedDate
    private Date createdAt;
}
