package com.example.fullstack.project;

import com.example.fullstack.task.Task;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.vertx.VertxContextSupport;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class ProjectResourceTest {

    @Test
    @TestSecurity(user = "user", roles = "user")
    void list() {
        given()
            .when().get("/api/v1/projects")
            .then()
            .statusCode(200)
            .body("$", hasItem(allOf(hasEntry("name", "Work"))));
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    void create() {
        given()
            .body("{\"name\":\"project-create\"}")
            .contentType(ContentType.JSON)
            .when().post("/api/v1/projects")
            .then()
            .statusCode(201)
            .body(
                "name", is("project-create"),
                "created", not(emptyString())
            );
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    void createDuplicate() {
        given()
            .body("{\"name\":\"create-existent\"}")
            .contentType(ContentType.JSON)
            .post("/api/v1/projects");
        given()
            .body("{\"name\":\"create-existent\"}")
            .contentType(ContentType.JSON)
            .when().post("/api/v1/projects")
            .then()
            .statusCode(409);
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    void update() {
        var toUpdate = given().body("{\"name\":\"to-update\"}").contentType(ContentType.JSON)
            .post("/api/v1/projects").as(Project.class);
        toUpdate.name = "updated";
        given()
            .body(toUpdate)
            .contentType(ContentType.JSON)
            .when().put("/api/v1/projects/" + toUpdate.id)
            .then()
            .statusCode(200)
            .body(
                "name", is("updated"),
                "version", is(toUpdate.version + 1)
            );
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    void updateNotFound() {
        given()
            .body("{\"name\":\"to-update\"}")
            .contentType(ContentType.JSON)
            .when().put("/api/v1/projects/1337")
            .then()
            .statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = "user")
    void updateUnauthorized() {
        given()
            .body("{\"name\":\"to-update\"}")
            .contentType(ContentType.JSON)
            .when().put("/api/v1/projects/0")
            .then()
            .statusCode(401);
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    void delete() throws Throwable {
        var toDelete = given().body("{\"name\":\"to-delete\"}").contentType(ContentType.JSON)
            .post("/api/v1/projects").as(Project.class);
        var dependentTask = given()
            .body("{\"title\":\"dependent-task\",\"project\":{\"id\":" + toDelete.id + "}}").contentType(ContentType.JSON)
            .post("/api/v1/tasks").as(Task.class);
        given()
            .when().delete("/api/v1/projects/" + toDelete.id)
            .then()
            .statusCode(204);
        assertThat(VertxContextSupport
                       .subscribeAndAwait(() -> Panache.withSession(() -> Task.<Task>findById(dependentTask.id)))
                       .project, nullValue());
    }
}
