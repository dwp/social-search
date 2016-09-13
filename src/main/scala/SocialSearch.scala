import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, scaladsl}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.slf4j.LoggerFactory
import play.api.libs.json._
import play.api.libs.ws.ahc.AhcWSClient
import play.api.mvc.MultipartFormData.DataPart
import wabisabi.Client

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.io.Source
import scala.io.StdIn.readLine

import models.QueryResponse
import models.IndexableMessage

object SocialSearch extends PlayJsonSupport {

  val config = ConfigFactory.load();

  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher
  implicit val timeout = Timeout(10.seconds)

  lazy val httpClient = AhcWSClient()
  lazy val esClient = new Client(config.getString("search.endpoint"))
  lazy val logger = LoggerFactory.getLogger(this.getClass)

  // Use a multiplier to increase scores for multiple results from a user
  val Multiplier = 1.01

  val QueryTemplate: String = """{
                                |  "size": 20,
                                |  "query": {
                                |    "multi_match": {
                                |      "query": "[KEYWORDS]",
                                |      "fields": [ "content", "content.shingles^2", "concepts^2", "entities^2" ]
                                |    }
                                |  }
                                |}""".stripMargin

  def terminateAll() = {
    Client.shutdown()
    httpClient.close()
    actorSystem.terminate()
  }

  /** Check that the web service is running: /ping */
  val pingRoute: Route = pathPrefix("ping") {
    get {
      complete("pong")
    }
  }

  /** Submit a question to find the most appropriate users: /ask?q=question+goes+here */
  val askRoute: Route = pathPrefix("ask") {
    get {
      parameter('q) {
        question =>
          val response: Future[QueryResponse] = extractTopics(question).flatMap {
            case (entities, concepts) =>
              bestUsers(question).map {
                users =>
                  QueryResponse(question, entities, concepts, users)
              }
          }

          onSuccess(response) { r => complete(r) }
      }
    }
  }

  /** Index an individual message into Elastic Search. */
  val indexRoute: Route = pathPrefix("messages") {
    post {
      entity(as[IndexableMessage]) {
        message =>
          val futureResponse = extractTopics(message.text).flatMap {
            case (entities, concepts) =>
              esClient.index(
                index = "messages",
                `type` = "slack",
                id = Some(message.id),
                data = Json.obj(
                  "content" -> message.text,
                  "user_id" -> message.user_id,
                  "user_name" -> message.user_name,
                  "concepts" -> concepts,
                  "entities" -> entities).toString)
          }
          onSuccess(futureResponse) {
            r => complete(if (r.getStatusCode == 201) Created else BadRequest)
          }
      }
    }
  }

  /** Query ElasticSearch with the question to find the users best suited to answer it. */
  def bestUsers(question: String, userField: String = "user_id"): Future[Seq[(String, Double)]] = {
    val stopwords = Source.fromInputStream(this.getClass.getResourceAsStream("/stopwords")).getLines().toSet
    val parts = question.replaceAll("[.,;'\"?!()]", "").toLowerCase.split(" ")
    val keywords = parts.filter(part => !stopwords.contains(part))
    esClient.search("messages", QueryTemplate.replace("[KEYWORDS]", keywords.mkString(" "))).map {
      esResponse =>
        val result = Json.parse(esResponse.getResponseBody)
        (result \\ userField).zip(result \\ "_score")
          .map(kv => kv._1.as[String] -> kv._2.as[Double])
          .groupBy(_._1)
          .map(kv => kv._1 -> kv._2.max._2 * scala.math.pow(Multiplier, kv._2.size))
          .toSeq.sortWith(_._2 > _._2)
    }
  }

  /** Run a question through Meaning Cloud's topic API to extract entities and concepts. */
  def extractTopics(question: String): Future[(Set[String], Set[String])] = {
    httpClient.url(config.getString("topic.endpoint"))
      .post(scaladsl.Source(
        DataPart("key", config.getString("topic.apikey")) ::
        DataPart("tt", "ec") ::
        DataPart("lang", "en") ::
        DataPart("txt", question) :: List()))
      .map {
        res =>
          val json = Json.parse(res.body)
          val entities = (json \ "entity_list" \\ "form").map(_.as[String].toLowerCase).toSet
          val concepts = (json \ "concept_list" \\ "form").map(_.as[String].toLowerCase).toSet
          (entities, concepts)
      }
  }

  /** Start and maintain a running interactive query tool for console users: requires -i flag. */
  def cli(): Unit = {
    print("Please enter a question: ")
    val question = readLine

    extractTopics(question).map {
      case (entities, concepts) =>
        if (entities.isEmpty)
          println("No entities identified in your question")
        else
          println(s"Entities identified from your question: ${entities.mkString(", ")}")

        if (concepts.isEmpty)
          println("No concepts identified in your question")
        else
          println(s"Concepts identified from your question: ${concepts.mkString(", ")}")

        // Note, we identify concepts above, just to determine what
        // concepts/entities have been identified, however, these are
        // not used in the actual query to ES. The question, as
        // entered by the user, is submitted in the query.

        bestUsers(question, "user_name").map {
          users =>
            println("Users best suited to answer your question: ")
            users.foreach(u => println(s"${u._1} (${u._2})"))
            print("\n\n")
            cli()
        }
    }
  }

  def main(args: Array[String]): Unit = {

    val responseStatus: Future[Int] = esClient.verifyIndex("messages").flatMap {
      response =>
        if (response.getStatusCode == 200) {
          Future.successful(response.getStatusCode)
        } else {
          esClient.createIndex("messages", Some("""{"settings":{"index":{"number_of_shards":1},"analysis":{"filter":{"my_shingle_filter":{"type":"shingle","min_shingle_size":2,"max_shingle_size":4,"output_unigrams":false}},"analyzer":{"my_shingle_analyzer":{"type":"custom","tokenizer":"standard","filter":["lowercase","my_shingle_filter"]}}}}}""")).flatMap {
            r =>
              if (r.getStatusCode == 200) {
                esClient.putMapping(Seq("messages"), "slack", """{"slack":{"properties":{"content":{"type":"string","fields":{"shingles":{"type":"string","analyzer":"my_shingle_analyzer"}}}}}}""").map(_.getStatusCode)
              } else {
                Future.successful(r.getStatusCode)
              }
          }
        }
    }

    responseStatus.map {
      code =>
        if (code != 200) {
          logger.error("Unable to setup or verify that the indexes have been configured.")
          terminateAll()
        } else {
          if (args.contains("-i"))
            cli()
          else {
            val routes = pingRoute ~ askRoute ~ indexRoute
            Http().bindAndHandle(routes, "0.0.0.0", 8080) recover {
              case e =>
                terminateAll()
            }
          }
        }
    }
  }
}
