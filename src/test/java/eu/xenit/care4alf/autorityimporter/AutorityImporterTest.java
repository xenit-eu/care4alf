package eu.xenit.care4alf.autorityimporter;

import io.restassured.RestAssured;
import io.restassured.authentication.PreemptiveBasicAuthScheme;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

public class AutorityImporterTest {

    @BeforeClass
    public static void setup() {
        String port = System.getProperty("port");
        if (port == null) {
            RestAssured.port = 443;
        } else {
            RestAssured.port = Integer.valueOf(port);
        }

        String protocol = System.getProperty("protocol");
        if(protocol == null){
            protocol = "https";
        }

        String basePath = "/alfresco/s";
        RestAssured.basePath = basePath;
        String host = System.getProperty("host");
        if (host == null) {
            host = "localhost";
        }
        String baseUri = protocol+"://"+host;
        System.out.println("Integration test host: " + baseUri);
        RestAssured.baseURI = baseUri;

        PreemptiveBasicAuthScheme authScheme = new PreemptiveBasicAuthScheme();
        String username = System.getProperty("username");
        if(username == null ){
            authScheme.setUserName("admin");
        } else {
            authScheme.setUserName(username);
        }
        String password = System.getProperty("password");
        if(password == null ){
            authScheme.setPassword("admin");
        } else {
            authScheme.setPassword(password);
        }
        RestAssured.authentication = authScheme;
        RestAssured.defaultParser = Parser.JSON;

    }

    @Test
    public void testOneGroupWithOneUser() throws JSONException, InterruptedException {
        loadTestUsers();

        JSONArray config = new JSONArray();
        JSONObject group = new JSONObject();
        group.put("name", "HELLO123");
        JSONArray empty = new JSONArray();
        JSONArray users = new JSONArray();
        users.put("user1");
        group.put("groups", empty);
        group.put("users", users);
        config.put(group);

        given()
                .when()
                .body(config.toString())
                .post("/xenit/care4alf/authorityimporter/import")
                .then()
                .statusCode(200);

        given()
                .when()
                .accept(ContentType.JSON)
                .get("/xenit/care4alf/authorityexplorer/groups")
                .then()
                .statusCode(200)
                //.contentType(ContentType.JSON)
                .body("name", hasItem(equalTo("GROUP_HELLO123")))
                .body("find {it.name == 'GROUP_HELLO123'}.users", containsInAnyOrder("user1"));
    }

    private void loadTestUsers() throws InterruptedException {
        Response post = given()
                .when()
                .multiPart(new File("src/test/resources/authorityimporter/ExampleUserUpload.csv"))
                .post("/api/people/upload");

        post.then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("data.totalUsers", equalTo(10));

        System.out.println("Sleeping 20 seconds to avoid not having the users in the index");
        Thread.sleep(20000);

    }


}
