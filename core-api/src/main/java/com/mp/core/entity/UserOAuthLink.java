package com.mp.core.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "user_oauth_links",
    schema = "sample_app",
    uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_uid"}))
public class UserOAuthLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String linkId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_uid", nullable = false)
    private String providerUid;

    private String email;

    private Date createdAt = new Date();
}
