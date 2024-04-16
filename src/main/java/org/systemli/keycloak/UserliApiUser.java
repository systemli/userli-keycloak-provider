package org.systemli.keycloak;

import java.util.List;
import lombok.Data;

@Data
public class UserliApiUser {
    private String id;
    private String email;
    // private String password;
    private List<String> roles;
    // private UserliDomain domain;

    public String getUsername() {
        return email.substring(0, email.indexOf('@'));
    }
}
