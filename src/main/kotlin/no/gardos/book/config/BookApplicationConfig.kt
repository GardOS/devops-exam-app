package no.gardos.book.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.gardos.book.model.Book
import no.gardos.book.model.BookRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.stereotype.Component
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

@Configuration
@EnableSwagger2
class BookApplicationConfig {

    @Bean
    fun swaggerApi(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .paths(PathSelectors.any())
                .apis(RequestHandlerSelectors.basePackage("no.gardos.book"))
                .build()
    }

    private fun apiInfo(): ApiInfo {
        return ApiInfoBuilder()
                .title("REST API for interacting with books")
                .version("1.0")
                .build()
    }

    @Bean(name = ["OBJECT_MAPPER_BEAN"])
    fun jsonObjectMapper(): ObjectMapper {
        return Jackson2ObjectMapperBuilder.json()
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .modules(JavaTimeModule())
                .build()
    }
}

@Component
internal class DataPreLoader : CommandLineRunner {
    @Autowired
    var bookRepo: BookRepository? = null

    override fun run(vararg args: String) {
        if (bookRepo!!.count() > 0)
            return

        bookRepo!!.save(Book(
                title = "The Phoenix Project:",
                author = "Gene Kim, Kevin Behr, Kim Spafford",
                edition = "5th Anniversary edition"
        ))

        bookRepo!!.save(Book(
                title = "The DevOps Handbook",
                author = "Gene Kim, Jez Humble, Patrick Debois, and John Willis"
        ))

        bookRepo!!.save(Book(
                title = "Clean Code",
                author = "Robert C. Martin"
        ))
    }
}