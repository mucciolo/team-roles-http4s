package com.mucciolo.config

case class AppConf(server: ServerConf, database: DatabaseConf)
case class ServerConf(host: String, port: Int, logHeaders: Boolean, logBody: Boolean)
case class DatabaseConf(driver: String, url: String, user: String, pass: String)