package no.exam.book.model

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.io.Serializable

@ApiModel("DTO for Book")
data class BookDto(
        @ApiModelProperty("Id of the book")
        var id: Long? = null,

        @ApiModelProperty("Title of the book")
        var title: String? = null,

        @ApiModelProperty("Author of the book")
        var author: String? = null,

        @ApiModelProperty("What edition the book is")
        var edition: String? = null
) : Serializable {
        override fun toString(): String {
                return "BookDto(id=$id, title=$title, author=$author, edition=$edition)"
        }
}