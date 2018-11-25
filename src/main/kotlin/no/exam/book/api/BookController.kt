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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
    var logger: Logger = LoggerFactory.getLogger(BookController::class.java)

    @Autowired
    private lateinit var registry: MetricRegistry
    @Autowired
    private lateinit var bookRepo: BookRepository

    @ApiOperation("Get all books")
    @GetMapping
    fun getAllBooks(): ResponseEntity<List<BookDto>> {
        logger.info("GET /books")
        registry.meter("books").mark()

        val books = bookRepo.findAll()
        logger.debug("GET /books. returning books:\n$books")

        return ResponseEntity.ok(BookConverter.transform(books))
    }

    @ApiOperation("Get book by id")
    @GetMapping(path = ["/{id}"])
    fun getBook(
            @ApiParam("Id of the book")
            @PathVariable("id")
            pathId: Long
    ): ResponseEntity<Any> {
        logger.info("GET /books/$pathId")
        registry.meter("books").mark()

        val book = bookRepo.findOne(pathId)

        if (book == null) {
            logger.debug("GET /books/$pathId. Book with id: $pathId not found")
            registry.counter("books-bad-input").inc()
            return ResponseEntity.status(404).body("Book with id: $pathId not found")
        }
        logger.debug("GET /books/$pathId. returning book:\n$book")

        return ResponseEntity.ok(BookConverter.transform(book))
    }

    @ApiOperation("Create new book")
    @PostMapping(consumes = [(MediaType.APPLICATION_JSON_VALUE)])
    fun createBook(
            @ApiParam("Book dto. Should not specify id")
            @RequestBody
            dto: BookDto
    ): ResponseEntity<Any> {
        logger.info("POST /books. Input:\n$dto")
        registry.meter("books").mark()

        //Id is auto-generated and should not be specified
        if (dto.id != null) {
            logger.debug("POST /books. Id specified")
            registry.counter("books-bad-input").inc()
            return ResponseEntity.status(400).body("Id should not be specified")
        }

        val book = bookRepo.save(
                Book(
                        title = dto.title,
                        author = dto.author,
                        edition = dto.edition
                )
        )
        logger.debug("POST /books. Book created:\n$book")

        return ResponseEntity.status(201).build()
    }

    @ApiOperation("Create or replace book")
    @PutMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun replaceBook(
            @ApiParam("The new book which will replace the old one")
            @RequestBody
            dto: BookDto
    ): ResponseEntity<Any> {
        logger.info("PUT /books. Input:\n$dto")
        registry.meter("books").mark()

        val book = Book(
                id = dto.id,
                title = dto.title,
                author = dto.author,
                edition = dto.edition
        )

        val status = if (book.id == null) 201 else 204

        val savedBook = bookRepo.save(book)
        logger.debug("PUT /books. Status: $status savedBook:\n$savedBook. ")

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
        logger.info("PATCH /books/$pathId. Input:\n$jsonBook")
        registry.meter("books").mark()

        if (!bookRepo.exists(pathId)) {
            logger.debug("PATCH /books/$pathId. Book with id: $pathId not found")
            registry.counter("books-bad-input").inc()
            return ResponseEntity.status(404).body("book with id: $pathId not found")
        }

        val book = bookRepo.findOne(pathId)
        logger.debug("PATCH /books/$pathId. Found book:\n$book")

        val jsonNode: JsonNode
        try {
            jsonNode = ObjectMapper().readValue(jsonBook, JsonNode::class.java)
        } catch (e: Exception) {
            logger.debug("PATCH /books/$pathId. JSON invalid")
            registry.counter("books-bad-input").inc()
            return ResponseEntity.status(400).build()
        }

        if (jsonNode.has("id")) {
            logger.debug("PATCH /books/$pathId. JSON invalid")
            registry.counter("books-bad-input").inc()
            return ResponseEntity.status(409).build()
        }

        if (jsonNode.has("title")) {
            val nameNode = jsonNode.get("title")
            when {
                nameNode.isNull -> book.title = null
                nameNode.isTextual -> book.title = nameNode.asText()
                else -> {
                    logger.debug("PATCH /books/$pathId. Invalid title")
                    registry.counter("books-bad-input").inc()
                    return ResponseEntity.status(400).build()
                }
            }
        }

        if (jsonNode.has("author")) {
            val nameNode = jsonNode.get("author")
            when {
                nameNode.isNull -> book.author = null
                nameNode.isTextual -> book.author = nameNode.asText()
                else -> {
                    logger.debug("PATCH /books/$pathId. Invalid author")
                    registry.counter("books-bad-input").inc()
                    return ResponseEntity.status(400).build()
                }
            }
        }

        if (jsonNode.has("edition")) {
            val nameNode = jsonNode.get("edition")
            when {
                nameNode.isNull -> book.edition = null
                nameNode.isTextual -> book.edition = nameNode.asText()
                else -> {
                    logger.debug("PATCH /books/$pathId. Invalid edition")
                    registry.counter("books-bad-input").inc()
                    return ResponseEntity.status(400).build()
                }
            }
        }

        bookRepo.save(book)
        logger.debug("PATCH /books. Updated book:\n$book")

        return ResponseEntity.status(204).build()
    }

    @ApiOperation("Delete existing book")
    @DeleteMapping(path = ["/{id}"])
    fun deleteBook(
            @ApiParam("Id of the book")
            @PathVariable("id")
            pathId: Long
    ): ResponseEntity<Any> {
        logger.info("DELETE /books/$pathId")
        registry.meter("books").mark()

        if (!bookRepo.exists(pathId)) {
            logger.debug("DELETE /books/$pathId. Book with id: $pathId not found")
            registry.counter("books-bad-input").inc()
            return ResponseEntity.status(404).body("Book with id: $pathId not found")
        }

        bookRepo.delete(pathId)
        logger.debug("DELETE /books/$pathId. Book deleted")

        return ResponseEntity.status(204).build()
    }

    //Catches exception and returns error status based on error
    //Because of how spring wraps exceptions, a "search" is done for constraint violations
    @ExceptionHandler(value = ([Exception::class]))
    fun handleValidationFailure(ex: Exception, response: HttpServletResponse): String? {
        var cause: Throwable? = ex
        for (i in 0..4) { //Iterate 5 times max, since it might have infinite depth
            if (cause is JavaxConstraintViolationException || cause is HibernateConstraintViolationException) {
                logger.warn("/books. Validation exception thrown. Cause: $cause")
                registry.counter("books-bad-input").inc()

                response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid request")

                return null
            }
            cause = cause?.cause
        }
        logger.error("/books. Exception thrown. Cause: $cause")
        registry.counter("books-error").inc()

        response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Something went wrong processing the request")
        return null
    }
}