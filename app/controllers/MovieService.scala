package controllers

/**
 * Created by siddhu on 05/10/15.
 */

import play.api._
import play.api.mvc._
import reactivemongo.bson.BSONDocument
import reactivemongo.core.commands.Count
import play.modules.reactivemongo.json._, ImplicitBSONHandlers._
import play.modules.reactivemongo.json.collection._
import play.modules.reactivemongo.{ReactiveMongoApi, MongoController, ReactiveMongoComponents}
import reactivemongo.api.gridfs.{ReadFile, GridFS}
import play.api.Logger
import play.api.Play.current
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ Action, Controller, Request }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsValue, Json, JsObject, JsString}
import scala.concurrent.Future
import javax.inject.Inject
import java.util.UUID
import MongoController.readFileReads
import models._

class MovieService @Inject() (val messagesApi: MessagesApi,val reactiveMongoApi: ReactiveMongoApi)
  extends Controller with MongoController with ReactiveMongoComponents {

  type JSONReadFile = ReadFile[JSONSerializationPack.type, JsString]
  val collection = db[JSONCollection]("movies")

  private val gridFS = reactiveMongoApi.gridFS
  gridFS.ensureIndex().onComplete {
    case index =>
      Logger.info(s"Checked index, result is $index")
  }

  def createMovie = Action { request =>
    implicit val messages = messagesApi.preferred(request)
    Ok(views.html.createMovie(Movie.MovieForm))
  }

  def addMovie = Action.async(gridFSBodyParser(gridFS)) { implicit request =>

    implicit val messages = messagesApi.preferred(request)
    var uid = Some(UUID.randomUUID().toString)
    val futureFile = request.body.files.head.ref
    futureFile.onFailure {
      case err => err.printStackTrace()
    }

    val futureUpdate = for {
      file <- { println("_0"); futureFile }
      // here, the file is completely uploaded, so it is time to update the article
      updateResult <- {
        println("_1")
        gridFS.files.update(
          Json.obj("_id" -> file.id),
          Json.obj("$set" -> Json.obj("movie_id" -> uid)))
      }
    } yield updateResult
    futureUpdate.map { _ =>
      ""
    }.recover {
      case e => InternalServerError(e.getMessage())
    }

    Movie.MovieForm.bindFromRequest.fold(
      errors => Future.successful(
        Ok(views.html.createMovie(errors))),

      // if no error, then insert the user into the 'account' collection
      movie => collection.insert(movie.copy(
        id = uid)
      ).map(_ => Redirect(routes.MovieService.createMovie))
    )
  }

}
