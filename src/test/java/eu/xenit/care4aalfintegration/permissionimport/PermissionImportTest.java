package eu.xenit.care4aalfintegration.permissionimport;

import io.restassured.RestAssured;
import io.restassured.authentication.PreemptiveBasicAuthScheme;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsEqual.equalTo;

import java.io.File;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.core.IsEqual.equalTo;


public class PermissionImportTest {

	@BeforeClass
	public static void setup() throws InterruptedException {
		String port = System.getProperty("port");
		RestAssured.port = port == null ? 8080 : Integer.valueOf(port);

		String protocol = System.getProperty("protocol");
		if (protocol == null) {
			protocol = "http";
		}

		String basePath = "/alfresco/s";
		RestAssured.basePath = basePath;
		RestAssured.useRelaxedHTTPSValidation();
		String host = System.getProperty("host");
		if (host == null) {
			host = "localhost";
		}
		String baseUri = protocol + "://" + host;
		System.out.println("Integration test host: " + baseUri);
		RestAssured.baseURI = baseUri;

		PreemptiveBasicAuthScheme authScheme = new PreemptiveBasicAuthScheme();
		String username = System.getProperty("username");
		authScheme.setUserName(username != null ? username : "admin");
		String password = System.getProperty("password");
		authScheme.setPassword(password == null ? password : "admin");
		RestAssured.authentication = authScheme;
		
		RestAssured.defaultParser = Parser.JSON;
		loadTestUsers();

	}

	private void checkFoldersPermissions() {

		/*
			To be checked (cfr src/test/resources/permissionimport/test.xlsx)
		 
			EMAIL_CONTRIBUTORS	Consumer	/test/folder1	FALSE
			SITE_ADMINISTRATORS	Contributor	/test/folder2	FALSE
			ALFRESCO_SEARCH_ADMINISTRATORS	Coordinator	/test/folder2/folder3	TRUE
			ALFRESCO_MODEL_ADMINISTRATORS	Editor	/test/folder4	FALSE		  
		 */
		
		given().when().param("path", "test/folder1").get("/xenit/care4alf/permissionimport/permissions")
		.then().statusCode(200)
		    .body("size()", equalTo(1))
			.body("[0].permission", equalTo("Consumer"))
			.body("[0].authority", equalTo("GROUP_EMAIL_CONTRIBUTORS"))
			.body("[0].inherited", equalTo(false))
		;

		given().when().param("path", "test/folder2").get("/xenit/care4alf/permissionimport/permissions")
		.then().statusCode(200)
		    .body("size()", equalTo(1))
			.body("[0].permission", equalTo("Contributor"))
			.body("[0].authority", equalTo("GROUP_SITE_ADMINISTRATORS"))
			.body("[0].inherited", equalTo(false))
		;

		given().when().param("path", "test/folder2/folder3").get("/xenit/care4alf/permissionimport/permissions")
		.then().statusCode(200)
		    .body("size()", equalTo(2))
			.body("[0].permission", equalTo("Coordinator"))
			.body("[0].authority", equalTo("GROUP_ALFRESCO_SEARCH_ADMINISTRATORS"))
			.body("[0].inherited", equalTo(false))
			.body("[1].permission", equalTo("Contributor"))
			.body("[1].authority", equalTo("GROUP_SITE_ADMINISTRATORS"))
			.body("[1].inherited", equalTo(true))
		;
		
		given().when().param("path", "test/folder4").get("/xenit/care4alf/permissionimport/permissions")
		.then().statusCode(200)
		    .body("size()", equalTo(1))
			.body("[0].permission", equalTo("Editor"))
			.body("[0].authority", equalTo("GROUP_ALFRESCO_MODEL_ADMINISTRATORS"))
			.body("[0].inherited", equalTo(false))
		;
		
	}
	
	
	@Test
    public void testImportPermissions() throws JSONException {

		// create groups of users
        JSONArray config = new JSONArray();
        config.put(getGroupConfig("EMAIL_CONTRIBUTORS", "user1", "user2"));
        config.put(getGroupConfig("SITE_ADMINISTRATORS", "user3", "user3"));
        config.put(getGroupConfig("ALFRESCO_SEARCH_ADMINISTRATORS", "user5", "user6"));
        config.put(getGroupConfig("ALFRESCO_MODEL_ADMINISTRATORS", "user7", "user8"));
        config.put(getGroupConfig("TEST_GROUP", "user9", "user10"));

        System.out.println("Adding groups...   \n" + config.toString());
        given()
        .when()
                .body(config.toString())
                .header("Content-Type", "application/json")
                .post("/xenit/care4alf/authorityimporter/import")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("response", equalTo(true));
        
        System.out.println("groups added");
		
		given().when().multiPart(new File("src/test/resources/permissionimport/test.xlsx")).param("removeFirst", false)
				.post("/xenit/care4alf/permissionimport/importpermissions")
				.then()
				.statusCode(200);
		
		checkFoldersPermissions();

		given().when().multiPart(new File("src/test/resources/permissionimport/test.xlsx")).param("removeFirst", true) // !!
		.post("/xenit/care4alf/permissionimport/importpermissions")
		.then()
		.statusCode(200);

		// should be the same permission sets as we removed the previous one.
		checkFoldersPermissions();
		
		
    }
	
    private JSONObject getGroupConfig(String groupName, String... users) throws JSONException {
        JSONObject group = new JSONObject();
        group.put("name", groupName);
        group.put("groups", new JSONArray());
        JSONArray usersJ = new JSONArray();
        for (String user: users) {
            usersJ.put(user);
        }
        group.put("users", usersJ);
        return group;
    }	
	
	private static void loadTestUsers() throws InterruptedException {
		Response post = given().when().multiPart(new File("src/test/resources/authorityimporter/ExampleUserUpload.csv"))
				.post("/api/people/upload");

		post.then().statusCode(200).contentType(ContentType.JSON).body("data.totalUsers", equalTo(10));
	}

}
