package cn.easyact.events

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.{Context, LambdaLogger, RequestHandler}
import scalaz.Free

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.sys.env

class ApiGatewayHandler extends RequestHandler[APIGatewayProxyRequestEvent, ApiGatewayResponse] {
  implicit def f[A](eventRepoF: EventRepoF[A]): EventRepo[A] = Free.liftF(eventRepoF)

  private val scalaMapper = {
    import com.fasterxml.jackson.databind.ObjectMapper
    import com.fasterxml.jackson.module.scala.DefaultScalaModule
    new ObjectMapper().registerModule(new DefaultScalaModule)
  }

  def handleRequest(req: APIGatewayProxyRequestEvent, context: Context): ApiGatewayResponse = {
    val log: LambdaLogger = context.getLogger
    log.log(s"env: $env")
    log.log(s"req: $req")
    val cmd = req.getHttpMethod match {
      case "POST" => StoreJsonSeq(req.getBody)
      case "GET" => Get(req.getPathParameters.get("id"))
      case "DELETE" => Delete(req.getPathParameters.get("id"))
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
