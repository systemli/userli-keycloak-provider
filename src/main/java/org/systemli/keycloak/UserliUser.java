package org.systemli.keycloak;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@NamedQuery(name = "findUserByEmailAndDomain", query = "select u from UserliUser u where u.email = :email and u.domain = :domain")
@NamedQuery(name = "findUsersByEmailAndDomain", query = "select u from UserliUser u where u.email like :email and u.domain = :domain")
@NamedQuery(name = "findUserByUsernameAndDomain", query = "select u from UserliUser u where u.email like :username and u.domain = :domain")
@NamedQuery(name = "findUserByIdAndDomain", query = "select u from UserliUser u where u.id = :id and u.domain = :domain")
@NamedQuery(name = "countUsersByDomain", query = "select count(u) from UserliUser u where u.domain = :domain")
@Table(name = "virtual_users")
@Entity
public class UserliUser {
    @Id
    @NonNull
    private String id;
    @NonNull
    private String email;
    @NonNull
    private String password;
    @NonNull
    private Boolean deleted;
    @ManyToOne
    @JoinColumn(name = "domain_id")
    private UserliDomain domain;

    public String getUsername() {
        return email.substring(0, email.indexOf('@'));
    }
}
