package hello

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import org.apache.logging.log4j.{LogManager, Logger}

import scala.jdk.CollectionConverters._

class ApiGatewayHandler extends RequestHandler[APIGatewayProxyRequestEvent, ApiGatewayResponse] {
  val scalaMapper = {
    import com.fasterxml.jackson.databind.ObjectMapper
    import com.fasterxml.jackson.module.scala.DefaultScalaModule
    new ObjectMapper().registerModule(new DefaultScalaModule)
  }

  def handleRequest(input: APIGatewayProxyRequestEvent, context: Context): ApiGatewayResponse = {
    val headers = Map("x-custom-response-header" -> "my custom response header value")
    val in = scalaMapper.writeValueAsString(input)
    context.getLogger.log(s"request: $in")
    val logger: Logger = LogManager.getLogger(getClass)
    logger.debug(s"request: $in")
    ApiGatewayResponse(200, in,
      headers.asJava,
      base64Encoded = true)
  }
}
