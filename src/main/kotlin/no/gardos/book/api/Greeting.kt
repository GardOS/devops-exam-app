package no.gardos.book.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.hibernate.exception.ConstraintViolationException as HibernateConstraintViolationException
import javax.validation.ConstraintViolationException as JavaxConstraintViolationException

@Api(value = "/", description = "Landing page")
@RequestMapping(
        path = ["/"],
        produces = [(MediaType.TEXT_HTML_VALUE)]
)
@RestController
class Greeting {
    @ApiOperation("Greet the user")
    @GetMapping
    fun getAllBooks(): String {
        return getHtml()
    }

    fun getHtml(): String {
        return """
<!DOCTYPE html>
<html lang="en">
    <head>
        <style>
            body {font-family: sans-serif; padding-left: 25px;}
        </style>
        <title>Exam App</title>
    </head>
    <body>
        <h1>Welcome to the book app/API</h1>
        <h2>You can perform rest operations under "/books"</h2>
        <h2>
            For more info, check out: <a href="./swagger-ui.html">API-doc</a>
        </h2>
    </body>
</html>
        """.trimIndent()
    }
}