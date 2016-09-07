import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.stream.{ActorMaterializer, scaladsl}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import play.api.libs.json.{JsArray, Json}
import play.api.libs.ws.ahc.AhcWSClient
import play.api.mvc.MultipartFormData.DataPart
import wabisabi.Client

//import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.io.Source
import scala.io.StdIn.readLine

object Query {

  val config = ConfigFactory.load();

  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = actorSystem.dispatcher
  implicit val timeout = Timeout(10.seconds)

  lazy val httpClient = AhcWSClient()
  lazy val esClient = new Client(config.getString("search.endpoint"))

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

  val pingRoute: Route = pathPrefix("ping") {
    get {
      complete("pong")
    }
  }

  val askRoute: Route = pathPrefix("ask") {
    get {
      parameter('q) {
        question =>
          
          complete(s"The question is: $question")
      }
    }
  }

  def bestUsers(question: String): Future[Seq[(String, Double)]] = {
    esClient.search("messages", QueryTemplate.replace("[KEYWORDS]", question)).map {
      esResponse =>
        val result = Json.parse(esResponse.getResponseBody)
        (result \\ "user_name").zip(result \\ "_score")
          .map(kv => kv._1.as[String] -> kv._2.as[Double])
          .groupBy(_._1)
          .map(kv => kv._1 -> kv._2.max._2 * scala.math.pow(Multiplier, kv._2.size))
          .toSeq.sortWith(_._2 > _._2)
    }
  }

  def extractTopics(question: String): Future[(Set[String], Set[String])] = {
    // run question through topic API to extract concepts/entities
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

  def cli(): Unit = {
    print("Please enter a question: ")
    val question = readLine

    // run question through topic API to extract concepts/entities
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

        println("Performing query against ElasticSearch using raw input...")

        bestUsers(question).map {
          users =>
            println("Users best suited to answer your question: ")
            users.foreach(u => println(s"${u._1} (${u._2})"))
            print("\n\n")
            cli()
        }
    }
  }

  def main(args: Array[String]): Unit = {
    if (args.contains("-i"))
      cli()
    else {
      Http().bindAndHandle(pingRoute ~ askRoute, "localhost", 8080) recover {
        case e =>
          terminateAll()
      }
    }
  }
}
