package cn.easyact.events

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.{Context, LambdaLogger, RequestHandler}
import scalaz.Free

import scala.jdk.CollectionConverters._
import scala.language.implicitConversions
import scala.sys.env

class PutEventsHandler extends RequestHandler[APIGatewayProxyRequestEvent, ApiGatewayResponse] {
  implicit def f[A](eventRepoF: EventRepoF[A]): EventRepo[A] = Free.liftF(eventRepoF)

  def handleRequest(req: APIGatewayProxyRequestEvent, context: Context): ApiGatewayResponse = {
    val log: LambdaLogger = context.getLogger
    log.log(s"env: $env")
    log.log(s"req: $req")
    val cmd = req.getHttpMethod match {
      case "POST" => StoreJsonSeq(req.getBody)
      case "GET" => Get(req.getPathParameters.get("email"))
    }
    val r = EventRepoDynamoDB(log).apply(cmd).unsafePerformSync
    log.log(s"outcome: $r")
    ApiGatewayResponse(200, r.toString,
      Map("x-custom-response-header" -> "my custom response header value").asJava,
      base64Encoded = true)
  }
}
