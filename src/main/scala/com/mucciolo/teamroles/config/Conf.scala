package com.mucciolo.teamroles.config

import org.http4s.Uri

final case class ServerConf(host: String, port: Int, logHeaders: Boolean, logBody: Boolean)
final case class DatabaseConf(driver: String, url: String, user: String, pass: String)
final case class UserTeamsClientConf(origin: Uri)
final case class AppConf(server: ServerConf, database: DatabaseConf, userTeamsClient: UserTeamsClientConf)
