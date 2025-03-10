package com.example.fullstack.user;

import com.example.fullstack.project.Project;
import com.example.fullstack.task.Task;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hibernate.ObjectNotFoundException;

import java.util.List;

@SuppressWarnings("CdiInjectionPointsInspection")
@ApplicationScoped
public class UserService {

    private final JsonWebToken jwt;

    @Inject
    public UserService(JsonWebToken jwt) {
        this.jwt = jwt;
    }

    public Uni<User> findById(long id) {
        return User.<User>findById(id)
            .onItem().ifNull().failWith(() -> new ObjectNotFoundException(id, "User"));
    }

    @SuppressWarnings("unused")
    public Uni<User> findByName(String name) {
        return User.find("name", name).firstResult();
    }

    public Uni<List<User>> list() {
        return User.listAll();
    }

    @WithTransaction
    public Uni<User> create(User user) {
        user.password = BcryptUtil.bcryptHash(user.password);
        return user.persistAndFlush();
    }

    @WithTransaction
    public Uni<User> update(User user) {
        return findById(user.id)
            .chain(u -> {
                user.setPassword(u.password);
                return User.getSession();
            })
            .chain(session -> session.merge(user));
    }

    @WithTransaction
    public Uni<Void> delete(long id) {
        return findById(id)
            .chain(u -> Uni.combine().all().unis(
                           Task.delete("user.id", u.id),
                           Project.delete("user.id", u.id)
                       ).asTuple()
                       .chain(t -> u.delete())
            );
    }

    public Uni<User> getCurrentUser() {
        return findByName(jwt.getName());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean matches(User user, String password) {
        return BcryptUtil.matches(password, user.password);
    }

    @WithTransaction
    public Uni<User> changePassword(String currentPassword, String newPassword) {
        return getCurrentUser()
            .chain(Unchecked.function(user -> {
                if (!matches(user, currentPassword)) {
                    throw new ClientErrorException("Current password does not match", Response.Status.CONFLICT);
                }
                user.setPassword(BcryptUtil.bcryptHash(newPassword));
                return user.persistAndFlush();
            }));
    }
}
