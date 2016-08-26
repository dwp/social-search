
object SlackTypeImplicits {
  import play.api.libs.json.Json
  implicit lazy val slackMessageReactionFormat= Json.format[SlackMessageReaction]
  implicit lazy val slackMessageFormat = Json.format[SlackMessage]
  implicit lazy val slackUserFormat = Json.format[SlackUser]
  implicit lazy val slackTeamFormat = Json.format[SlackTeam]
}

case class SlackTeam(id: String, name: String, domain: String, email_domain: String, users: Seq[SlackUser])

case class SlackUser(
  id: String,
  name: String,
  email: String,
  has_2fa: Boolean,
  is_admin: Boolean,
  is_owner: Boolean,
  is_bot: Boolean,
  deleted: Boolean,
  messages: Seq[SlackMessage])

case class SlackMessage(
  id: String,
  timestamp: String,
  `type`: String,
  sub_type: Option[String],
  channel: String,
  text: String,
  reactions: Seq[SlackMessageReaction],
  is_bot: Boolean)

case class SlackMessageReaction(name: String, count: Int)
