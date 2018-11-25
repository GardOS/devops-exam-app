package no.exam.book

import io.restassured.RestAssured
import io.restassured.RestAssured.get
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.fail
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.context.embedded.LocalServerPort
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [(BookApplication::class)])
class GreetingTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun initClass() {
            RestAssured.baseURI = "http://localhost/"
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        }
    }

    @Before
    fun init() {
        RestAssured.port = port
    }

    @LocalServerPort
    protected var port = 0

    @Test
    fun test_fail() {
        fail()
    }

    @Test
    fun greeting_ok() {
        get().then().statusCode(200)
    }

    @Test
    fun greeting_hasContent() {
        val greeting = get().body().print()
        assertNotNull(greeting)
    }
}