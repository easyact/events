package cn.easyact.events

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.{Context, LambdaLogger, RequestHandler}
import scalaz.Free

import scala.jdk.CollectionConverters._
import scala.language.implicitConversions
import scala.sys.env

sealed trait ApiGatewayHandler extends RequestHandler[APIGatewayProxyRequestEvent, ApiGatewayResponse] {
  implicit def f[A](eventRepoF: EventRepoF[A]): EventRepo[A] = Free.liftF(eventRepoF)

  def handleRequest(req: APIGatewayProxyRequestEvent, context: Context): ApiGatewayResponse = {
    val log: LambdaLogger = context.getLogger
    log.log(s"env: $env")
    log.log(s"req: $req")
    val r = EventRepoDynamoDB(log).apply(cmd(req)).unsafePerformSync
    log.log(s"outcome: $r")
    ApiGatewayResponse(200, r.toString,
      Map("x-custom-response-header" -> "my custom response header value").asJava,
      base64Encoded = true)
  }

  def cmd(req: APIGatewayProxyRequestEvent): EventRepo[_]
}

case class Post() extends ApiGatewayHandler {
  override def cmd(req: APIGatewayProxyRequestEvent): EventRepo[Any] = StoreJsonSeq(req.getBody)
}

case class Get() extends ApiGatewayHandler {
  override def cmd(req: APIGatewayProxyRequestEvent): EventRepo[Any] = GetEvents(req.getPathParameters.get("email"))
}