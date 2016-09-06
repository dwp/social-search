import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, scaladsl}
import com.typesafe.config.ConfigFactory
import play.api.libs.json.{JsArray, Json}
import play.api.libs.ws.ahc.AhcWSClient
import play.api.mvc.MultipartFormData.DataPart
import wabisabi.Client

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.io.StdIn.readLine

object Query {

  // Use a multiplier to increase scores for multiple results from a user
  val Multiplier = 1.01

  val config = ConfigFactory.load();

  val QueryTemplate: String = """{
                                |  "size": 20,
                                |  "query": {
                                |    "multi_match": {
                                |      "query": "[KEYWORDS]",
                                |      "fields": [ "content", "content.shingles^2", "concepts^2", "entities^2" ]
                                |    }
                                |  }
                                |}""".stripMargin

  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()

  lazy val httpClient = AhcWSClient()
  lazy val esClient = new Client(config.getString("search.endpoint"))

  def terminateAll() = {
    Client.shutdown()
    httpClient.close()
    actorSystem.terminate()
  }

  def main(args: Array[String]) = {
    ask()

    def ask(): Unit = {
      print("Please enter a question: ")
      val question = readLine

      // run question through topic API to extract concepts/entities
      httpClient.url(config.getString("topic.endpoint"))
        .post(scaladsl.Source(
          DataPart("key", config.getString("topic.apikey")) ::
            DataPart("tt", "ec") ::
            DataPart("lang", "en") ::
            DataPart("txt", question) :: List())).map {
        res =>
          val json = Json.parse(res.body)
          val entities = (json \ "entity_list" \\ "form").map(_.as[String].toLowerCase).toSet
          val concepts = (json \ "concept_list" \\ "form").map(_.as[String].toLowerCase).toSet

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

          esClient.search("messages", QueryTemplate.replace("[KEYWORDS]", question)).map {
            esResponse =>
              val result = Json.parse(esResponse.getResponseBody)

              val users: Seq[(String, Double)] = (result \\ "user_name").zip(result \\ "_score")
                .map(kv => kv._1.as[String] -> kv._2.as[Double])
                .groupBy(_._1)
                .map(kv => kv._1 -> kv._2.max._2 * scala.math.pow(Multiplier, kv._2.size))
                .toSeq.sortWith(_._2 > _._2)

              println("Users best suited to answer your question: ")
              users.foreach(u => println(s"${u._1} (${u._2})"))
              println("\n\n")

              ask()
          }
      }
    }
  }
}
