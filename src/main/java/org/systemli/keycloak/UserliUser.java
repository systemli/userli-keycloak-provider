package org.systemli.keycloak;

import java.util.List;
import lombok.Data;

@Data
public class UserliUser {
    private String id;
    private String email;
    private List<String> roles;

    public String getUsername() {
        return email.substring(0, email.indexOf('@'));
    }
}
