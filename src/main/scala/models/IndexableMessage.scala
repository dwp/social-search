package models

case class IndexableMessage(
  id: String,
  timestamp: String,
  text: String,
  user_id: String,
  user_name: String,
  team: String)

object IndexableMessage {
  import play.api.libs.json.Json
  implicit lazy val indexableMessageFormat = Json.format[IndexableMessage]
}
