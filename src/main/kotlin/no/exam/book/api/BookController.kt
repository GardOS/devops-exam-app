package no.exam.book.api

import com.codahale.metrics.MetricRegistry
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import no.exam.book.model.Book
import no.exam.book.model.BookConverter
import no.exam.book.model.BookDto
import no.exam.book.model.BookRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletResponse
import org.hibernate.exception.ConstraintViolationException as HibernateConstraintViolationException
import javax.validation.ConstraintViolationException as JavaxConstraintViolationException

@Api(value = "/books", description = "API for books")
@RequestMapping(
        path = ["/books"],
        produces = [(MediaType.APPLICATION_JSON_VALUE)]
)
@RestController
@Validated
class BookController {
    @Autowired
    private lateinit var registry: MetricRegistry
    @Autowired
    private lateinit var bookRepo: BookRepository

    @ApiOperation("Get all books")
    @GetMapping
    fun getAllBooks(): ResponseEntity<List<BookDto>> {
        registry.meter("books").mark()

        return ResponseEntity.ok(BookConverter.transform(bookRepo.findAll()))
    }

    @ApiOperation("Get book by id")
    @GetMapping(path = ["/{id}"])
    fun getBook(
            @ApiParam("Id of the book")
            @PathVariable("id")
            pathId: Long
    ): ResponseEntity<Any> {
        registry.meter("books").mark()

        val book = bookRepo.findOne(pathId)

        if(book == null){
            registry.counter("books-bad-input").inc()
            return ResponseEntity.status(404).body("book with id: $pathId not found")
        }

        return ResponseEntity.ok(BookConverter.transform(book))
    }

    @ApiOperation("Create new book")
    @PostMapping(consumes = [(MediaType.APPLICATION_JSON_VALUE)])
    fun createBook(
            @ApiParam("Book dto. Should not specify id")
            @RequestBody
            dto: BookDto
    ): ResponseEntity<Any> {
        registry.meter("books").mark()

        //Id is auto-generated and should not be specified
        if (dto.id != null) {
            registry.counter("books-bad-input").inc()
            return ResponseEntity.status(400).body("Id should not be specified")
        }

        bookRepo.save(
                Book(
                        title = dto.title,
                        author = dto.author,
                        edition = dto.edition
                )
        )

        return ResponseEntity.status(201).build()
    }

    @ApiOperation("Create or replace book")
    @PutMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun replaceBook(
            @ApiParam("The new book which will replace the old one")
            @RequestBody
            dto: BookDto
    ): ResponseEntity<Any> {
        registry.meter("books").mark()

        val book = Book(
                id = dto.id,
                title = dto.title,
                author = dto.author,
                edition = dto.edition
        )

        val status = if (book.id == null) 201 else 204

        bookRepo.save(book)

        return ResponseEntity.status(status).build()
    }

    @ApiOperation("Update an existing book")
    @PatchMapping(path = ["/{id}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun updateBook(
            @ApiParam("Id of the book")
            @PathVariable("id")
            pathId: Long,
            @ApiParam("Fields to change on the book. Id should not be specified")
            @RequestBody
            jsonBook: String
    ): ResponseEntity<Any> {
        registry.meter("books").mark()

        if (!bookRepo.exists(pathId)) {
            registry.counter("books-bad-input").inc()
            return ResponseEntity.status(404).body("book with id: $pathId not found")
        }

        val updatedBook = bookRepo.findOne(pathId)

        val jsonNode: JsonNode
        try {
            jsonNode = ObjectMapper().readValue(jsonBook, JsonNode::class.java)
        } catch (e: Exception) {
            registry.counter("books-bad-input").inc()
            return ResponseEntity.status(400).build()
        }

        if (jsonNode.has("id")) {
            registry.counter("books-bad-input").inc()
            return ResponseEntity.status(409).build()
        }

        if (jsonNode.has("title")) {
            val nameNode = jsonNode.get("title")
            when {
                nameNode.isNull -> updatedBook.title = null
                nameNode.isTextual -> updatedBook.title = nameNode.asText()
                else -> {
                    registry.counter("books-bad-input").inc()
                    return ResponseEntity.status(400).build()
                }
            }
        }

        if (jsonNode.has("author")) {
            val nameNode = jsonNode.get("author")
            when {
                nameNode.isNull -> updatedBook.author = null
                nameNode.isTextual -> updatedBook.author = nameNode.asText()
                else -> {
                    registry.counter("books-bad-input").inc()
                    return ResponseEntity.status(400).build()
                }
            }
        }

        if (jsonNode.has("edition")) {
            val nameNode = jsonNode.get("edition")
            when {
                nameNode.isNull -> updatedBook.edition = null
                nameNode.isTextual -> updatedBook.edition = nameNode.asText()
                else -> {
                    registry.counter("books-bad-input").inc()
                    return ResponseEntity.status(400).build()
                }
            }
        }

        bookRepo.save(updatedBook)

        return ResponseEntity.status(204).build()
    }

    @ApiOperation("Delete existing book")
    @DeleteMapping(path = ["/{id}"])
    fun deleteBook(
            @ApiParam("Id of the book")
            @PathVariable("id")
            pathId: Long
    ): ResponseEntity<Any> {
        registry.meter("books").mark()

        if (!bookRepo.exists(pathId)) {
            registry.counter("books-bad-input").inc()
            return ResponseEntity.status(404).body("Book with id: $pathId not found")
        }

        bookRepo.delete(pathId)

        return ResponseEntity.status(204).build()
    }

    //Catches exception and returns error status based on error
    //Because of how spring wraps exceptions, a "search" is done for constraint violations
    @ExceptionHandler(value = ([Exception::class]))
    fun handleValidationFailure(ex: Exception, response: HttpServletResponse): String {
        var cause: Throwable? = ex
        for (i in 0..4) { //Iterate 5 times max, since it might have infinite depth
            if (cause is JavaxConstraintViolationException || cause is HibernateConstraintViolationException) {
                response.status = HttpStatus.BAD_REQUEST.value()
                registry.counter("books-bad-input").inc()
                return "Invalid request"
            }
            cause = cause?.cause
        }

        registry.counter("books-error").inc()

        response.status = HttpStatus.INTERNAL_SERVER_ERROR.value()
        return "Something went wrong processing the request"
    }
}