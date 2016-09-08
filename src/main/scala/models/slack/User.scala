package models.slack

case class User(
  id: String,
  name: String,
  email: String,
  has_2fa: Boolean,
  is_admin: Boolean,
  is_owner: Boolean,
  is_bot: Boolean,
  deleted: Boolean,
  messages: Seq[Message])
