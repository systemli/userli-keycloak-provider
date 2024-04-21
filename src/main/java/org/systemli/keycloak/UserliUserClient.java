package org.systemli.keycloak;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface UserliUserClient {

        @GET
        @Path("/api/keycloak")
        List<UserliUser> getUsers(@QueryParam("search") String search, @QueryParam("first") int first, @QueryParam("max") int max);

        @GET
        @Path("/api/keycloak/count")
        Integer getUsersCount();

        @GET
        @Path("/api/keycloak/user/{id}")
        UserliUser getUserById(@PathParam("id") String id);

        @POST
        @Path("/api/keycloak/validate/${email}")
        Boolean validate(@PathParam("email") String email, @QueryParam("password") String password);

}
