package models.slack

object Implicits {
  import play.api.libs.json.Json
  implicit lazy val slackMessageReactionFormat= Json.format[MessageReaction]
  implicit lazy val slackMessageFormat = Json.format[Message]
  implicit lazy val slackUserFormat = Json.format[User]
  implicit lazy val slackChannelFormat = Json.format[Channel]
  implicit lazy val slackTeamFormat = Json.format[Team]
}
