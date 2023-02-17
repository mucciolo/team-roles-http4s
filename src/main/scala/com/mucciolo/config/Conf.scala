package com.mucciolo.config

import org.http4s.Uri

case class ServerConf(host: String, port: Int, logHeaders: Boolean, logBody: Boolean)
case class DatabaseConf(driver: String, url: String, user: String, pass: String)
case class UserTeamsClientConf(origin: Uri)
case class AppConf(server: ServerConf, database: DatabaseConf, userTeamsClient: UserTeamsClientConf)
