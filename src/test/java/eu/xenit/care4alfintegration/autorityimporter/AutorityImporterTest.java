package eu.xenit.care4alfintegration.autorityimporter;

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
import java.util.Random;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsEqual.equalTo;

public class AutorityImporterTest {

    @BeforeClass
    public static void setup() throws InterruptedException {
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
        RestAssured.useRelaxedHTTPSValidation();
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
        loadTestUsers();


    }

    @Test
    public void testOneGroupWithOneUser() throws JSONException {

        JSONArray config = new JSONArray();
        String groupName = "HELLO123";
        config.put(getGroupConfig(groupName, "user1"));

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

    @Test
    public void testMultiGroupMultiUser() throws JSONException {
        String groupName1 = getSaltString();
        String groupName2 = getSaltString();

        JSONArray config = new JSONArray();

        config.put(getGroupConfig(groupName1, "user1", "user2", "user3"));
        config.put(getGroupConfig(groupName2, "user4", "user5", "user6"));

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
                .body("find {it.name == 'GROUP_"+groupName1+"'}.users", containsInAnyOrder("user1", "user2", "user3"))
                .body("find {it.name == 'GROUP_"+groupName2+"'}.users", containsInAnyOrder("user4", "user5", "user6"));
    }

    private JSONObject getGroupConfig(String groupName, String... users) throws JSONException {
        JSONObject group = new JSONObject();
        group.put("name", groupName);
        group.put("groups", new JSONArray());
        JSONArray usersJ = new JSONArray();
        for (String user: users
             ) {
            usersJ.put(user);
        }
        group.put("users", usersJ);
        return group;
    }

    protected String getSaltString() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 18) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;

    }

    private static void loadTestUsers() throws InterruptedException {
        Response post = given()
                .when()
                .multiPart(new File("src/test/resources/authorityimporter/ExampleUserUpload.csv"))
                .post("/api/people/upload");

        post.then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("data.totalUsers", equalTo(10));

    }


}
