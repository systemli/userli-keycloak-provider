package org.systemli.keycloak;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@NamedQuery(name = "findByName", query = "select d from UserliDomain d where d.name = :name")
@Table(name = "virtual_domains")
@Entity
public class UserliDomain {
    @Id
    private String id;
    private String name;
}
