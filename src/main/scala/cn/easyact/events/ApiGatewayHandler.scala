package cn.easyact.events

import cn.easyact.events.ApiGatewayHandler.readUnUploadedCommands
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.{Context, LambdaLogger, RequestHandler}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import scalaz.Free

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.sys.env

class ApiGatewayHandler extends RequestHandler[APIGatewayProxyRequestEvent, ApiGatewayResponse] {
  implicit def f[A](eventRepoF: EventRepoF[A]): EventRepo[A] = Free.liftF(eventRepoF)

  def handleRequest(req: APIGatewayProxyRequestEvent, context: Context): ApiGatewayResponse = {
    import ApiGatewayHandler.scalaMapper
    val log: LambdaLogger = context.getLogger
    log.log(s"env: $env")
    log.log(s"req: $req")
    val uid = req.getPathParameters.get("id")
    val cmd: EventRepo[_] = req.getHttpMethod match {
      case "POST" =>
        val commands = readUnUploadedCommands(req.getBody)
        for {
          _ <- StoreJsonSeq(commands.commands)
          l <- Get(uid, commands.beginAt)
        } yield l
      case "GET" => Get(uid)
      case "DELETE" => Delete(uid)
    }
    log.log(s"Will exec cmd: $cmd")
    val r = EventRepoDynamoDB(log).apply(cmd).unsafePerformSync
    val resp = scalaMapper.writeValueAsString(r)
    log.log(s"outcome: $resp")
    ApiGatewayResponse(200, resp,
      Map("Content-Type" -> "application/json").asJava,
      base64Encoded = true)
  }
}

object ApiGatewayHandler {
  val scalaMapper: ObjectMapper = {
    import com.fasterxml.jackson.databind.ObjectMapper
    import com.fasterxml.jackson.module.scala.DefaultScalaModule
    new ObjectMapper().registerModule(new DefaultScalaModule)
  }

  def readUnUploadedCommands(body: String): UnUploadedCommands = {
    val node = scalaMapper.readTree(body)
    UnUploadedCommands(jsonNode2Strings(node.get("commands")), Option(node.get("beginAt")).map(_.asText))
  }

  def jsonToStrings(input: String): Array[String] = jsonNode2Strings(scalaMapper.readTree(input))

  private def jsonNode2Strings(node: JsonNode) = node.iterator().asScala.toArray.map(_.toString)
}

case class UnUploadedCommands(commands: Array[String], beginAt: Option[String] = None)