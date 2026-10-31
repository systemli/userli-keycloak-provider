package org.systemli.keycloak;

import java.util.List;

import lombok.Data;

@Data
public class UserliWebauthnCredential {
    private Integer id;
    private String name;
    private String credentialId;
    private List<String> transports;
    private String createdAt;
}
