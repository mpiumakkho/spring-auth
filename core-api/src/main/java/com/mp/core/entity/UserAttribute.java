package com.mp.core.entity;

import java.util.Date;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "user_attributes",
    schema = "sample_app",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "attr_key"}))
@EntityListeners(AuditingEntityListener.class)
public class UserAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String attributeId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "attr_key", nullable = false, length = 100)
    private String key;

    @Column(name = "attr_value", length = 500)
    private String value;

    @CreatedDate
    private Date createdAt;

    @LastModifiedDate
    private Date updatedAt;
}
