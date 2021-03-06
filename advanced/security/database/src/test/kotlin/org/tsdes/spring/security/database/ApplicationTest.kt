package org.tsdes.spring.security.database

import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matchers.contains
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import org.tsdes.spring.security.database.db.UserRepository

/**
 * Created by arcuri82 on 08-Nov-17.
 */
@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationTest {

    @Autowired
    private lateinit var userRepository: UserRepository

    @LocalServerPort
    private var port = 0


    @Before
    fun initialize() {
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = port
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    @Before
    fun clean() {
        userRepository.deleteAll()
    }


    @Test
    fun testUnauthorizedAccess() {

        given().accept("${ContentType.TEXT},*/*")
                .get("/resource")
                .then()
                .statusCode(401)
    }


    @Test
    fun testRegisterUser() {

        registerUser("foo", "bar")
    }

    private fun registerUser(id: String, password: String): String? {

        return given().contentType(ContentType.URLENC)
                .formParam("the_user", id)
                .formParam("the_password", password)
                .post("/signIn")
                .then()
                .statusCode(204)
                .extract().cookie("JSESSIONID")
    }

    @Test
    fun testFailDoubleRegistration() {

        val name = "foo"
        val pwd = "bar"

        given().contentType(ContentType.URLENC)
                .formParam("the_user", name)
                .formParam("the_password", pwd)
                .post("/signIn")
                .then()
                .statusCode(204)

        given().contentType(ContentType.URLENC)
                .formParam("the_user", name)
                .formParam("the_password", pwd)
                .post("/signIn")
                .then()
                .statusCode(400)
    }


    @Test
    fun testRegisterAndGetResource() {

        val name = "foo"
        val pwd = "bar"

        registerUser(name, pwd)

        given().accept("${ContentType.TEXT},*/*")
                .auth().basic(name, pwd)
                .get("/resource")
                .then()
                .statusCode(200)
    }

    @Test
    fun testAccessCookie() {

        val name = "foo"
        val pwd = "bar"

        registerUser(name, pwd)


        val jid = given().accept("${ContentType.TEXT},*/*")
                .auth().basic(name, pwd)
                .get("/resource")
                .then()
                .statusCode(200)
                .extract().cookie("JSESSIONID")


        //
        given().accept("${ContentType.TEXT},*/*")
                .cookie("JSESSIONID", jid)
                .get("/resource")
                .then()
                .statusCode(200)
    }

    @Test
    fun testSessionFixation() {

        val first = given().accept("${ContentType.TEXT},*/*")
                .get("/resource")
                .then()
                .statusCode(401)
                .cookie("JSESSIONID")
                .extract().cookie("JSESSIONID")

        /*
            SpringBoot will try to set a session cookie
         */
        assertNotNull(first)

        val second = given().accept("${ContentType.TEXT},*/*")
                .cookie("JSESSIONID", first)
                .get("/resource")
                .then()
                .statusCode(401)
                .extract().cookie("JSESSIONID")

        /*
            As we provided a valid session cookie, SpringBoot will
            use it, and not try to set a new one
         */
        assertNull(second)

        val name = "foo"
        val pwd = "bar"

        registerUser(name, pwd)

        val third = given().accept("${ContentType.TEXT},*/*")
                .auth().basic(name, pwd)
                .cookie("JSESSIONID", first)
                .get("/resource")
                .then()
                .statusCode(200)
                .extract().cookie("JSESSIONID")

        /*
            The previous session cookie was not authenticated.
            Now that we do authenticate, SpringBoot does create a
            new session cookie to prevent session fixation attacks
         */
        assertNotNull(third)
        assertNotEquals(third, first)
        assertNotEquals(third, second)
    }

    @Test
    fun testSignInCreateAuthenticatedSession() {

        val cookie = registerUser("hello", "world")
        assertNotNull(cookie)
    }

    @Test
    fun testSignInAndUseCookie() {

        val first = registerUser("a", "b")

        assertNotNull(first)

        val second = given().accept("${ContentType.TEXT},*/*")
                .cookie("JSESSIONID", first)
                .get("/resource")
                .then()
                .statusCode(200)
                .extract().cookie("JSESSIONID")

        assertNull(second)
    }

    @Test
    fun testUseBasicAfterSignIn() {

        val name = "foo"
        val pwd = "bar"

        val first = registerUser(name, pwd)

        val second = given().accept("${ContentType.TEXT},*/*")
                .auth().basic(name, pwd)
                .get("/resource")
                .then()
                .statusCode(200)
                .extract().cookie("JSESSIONID")

        assertNotNull(first)
        assertNotNull(second)
        assertNotEquals(first, second)
    }


    @Test
    fun testUseBasicAfterSignInWithSameCookie() {

        val name = "foo"
        val pwd = "bar"

        val first = registerUser(name, pwd)

        val second = given().accept("${ContentType.TEXT},*/*")
                .auth().basic(name, pwd)
                .cookie("JSESSIONID", first)
                .get("/resource")
                .then()
                .statusCode(200)
                .extract().cookie("JSESSIONID")

        assertNotNull(first)
        assertNull(second)
    }


    @Test
    fun testLogout() {

        val cookie = registerUser("a", "b")

        given().accept("${ContentType.TEXT},*/*")
                .cookie("JSESSIONID", cookie)
                .get("/resource")
                .then()
                .statusCode(200)

        given().post("/logout")
                .then()
                .statusCode(204)

        given().accept("${ContentType.TEXT},*/*")
                .cookie("JSESSIONID", cookie)
                .get("/resource")
                .then()
                .statusCode(200)

        given().cookie("JSESSIONID", cookie)
                .post("/logout")
                .then()
                .statusCode(204)

        given().accept("${ContentType.TEXT},*/*")
                .cookie("JSESSIONID", cookie)
                .get("/resource")
                .then()
                .statusCode(401)
    }


    @Test
    fun testLogin() {

        val name = "foo"
        val pwd = "bar"

        registerUser(name, pwd)

        given().get("/user")
                .then()
                .statusCode(401)

        given().auth().basic(name, pwd)
                .get("/user")
                .then()
                .statusCode(200)
                .cookie("JSESSIONID")
                .body("name", equalTo(name))
                .body("roles", contains("ROLE_USER"))
    }
}