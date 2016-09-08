package models

case class QueryResponse(
  question: String,
  entities: Set[String],
  concepts: Set[String],
  users: Seq[(String, Double)])

object QueryResponse {
  import play.api.libs.json.{Json, Writes}
  implicit lazy val userTupleWriter = Writes[(String, Double)](t => Json.obj("user_id" -> t._1, "score" -> t._2))
  implicit lazy val queryResponseWriter = Json.writes[QueryResponse]
}
