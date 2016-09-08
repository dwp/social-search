package models.slack

case class Message(
  id: String,
  timestamp: String,
  `type`: String,
  sub_type: Option[String],
  channel: String,
  text: String,
  reactions: Seq[MessageReaction],
  is_bot: Boolean)
