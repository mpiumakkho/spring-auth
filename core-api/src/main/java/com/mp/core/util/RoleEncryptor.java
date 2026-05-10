package com.mp.core.util;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mp.core.entity.Role;

@Component
public class RoleEncryptor {
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Encrypt roles array
    public String encryptRoles(List<Role> roles) throws Exception {
        String json = objectMapper.writeValueAsString(roles);
        return AESUtil.encrypt(json);
    }

    // Decrypt roles array
    public List<Role> decryptRoles(String encryptedRoles) throws Exception {
        String json = AESUtil.decrypt(encryptedRoles);
        return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, Role.class));
    }
} 