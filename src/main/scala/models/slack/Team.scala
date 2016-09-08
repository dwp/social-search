package models.slack

case class Team(
  id: String,
  name: String,
  domain: String,
  email_domain: String,
  channels: Seq[Channel],
  users: Seq[User])
