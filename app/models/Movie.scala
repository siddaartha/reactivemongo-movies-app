package models

/**
 * Created by siddhu on 05/10/15.
 */
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints.pattern
import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONObjectID}
import play.api.libs.json._
import java.util.Date

case class Movie(id: Option[String], title: String, genre: String, release_date: Date, rating:String)

object Movie{
  implicit object MovieWrites extends OWrites[Movie] {
    def writes(movie: Movie): JsObject = Json.obj(
      "_id" -> movie.id,
      "title" -> movie.title,
      "genre" -> movie.genre,
      "release_date" -> movie.release_date,
      "rating" -> movie.rating
    )
  }

  implicit object MovieReads extends Reads[Movie] {
    def reads(json: JsValue): JsResult[Movie] = json match {
      case obj: JsObject => try {
        val id = (obj \ "_id").asOpt[String]
        val title = (obj \ "title").as[String]
        val genre = (obj \ "genre").as[String]
        val release_date = (obj \ "release_date").as[Date]
        val rating = (obj \ "rating").as[String]

        JsSuccess(Movie(id, title, genre, release_date, rating))
      } catch {
        case cause: Throwable => JsError(cause.getMessage)
      }
      case _ => JsError("expected.jsobject")
    }
  }

  val MovieForm = Form(
    mapping(
      "id" -> optional(text verifying pattern(
        """[a-fA-F0-9]{24}""".r, error = "error.objectId")),
      "title" -> nonEmptyText,
      "genre" -> nonEmptyText,
      "release_date" -> date("yyyy-MM-dd"),
      "rating" -> nonEmptyText
    ) {
      (id, title, genre, release_date, rating) =>
        Movie(
          id,
          title,
          genre,
          release_date,
          rating
        )
    } { movie =>
      Some(
        (movie.id,
          movie.title,
          movie.genre,
          movie.release_date,
          movie.rating
          ))
    })
}
