package hello

import com.amazonaws.services.dynamodbv2.document.{BatchWriteItemOutcome, Item, TableWriteItems}
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.{LogManager, Logger}

import scala.jdk.CollectionConverters._
import scala.sys.env

class ApiGatewayHandler extends RequestHandler[APIGatewayProxyRequestEvent, ApiGatewayResponse] {
  val scalaMapper: ObjectMapper = {
    import com.fasterxml.jackson.databind.ObjectMapper
    import com.fasterxml.jackson.module.scala.DefaultScalaModule
    new ObjectMapper().registerModule(new DefaultScalaModule)
  }

  import com.amazonaws.services.dynamodbv2.document.{DynamoDB, Table}
  import com.amazonaws.client.builder.AwsClientBuilder
  import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
  import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder

  val client: AmazonDynamoDB = AmazonDynamoDBClientBuilder.standard.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-west-2")).build

  val dynamoDB = new DynamoDB(client)

  private val tableName: String = env.getOrElse("events_table", "events")
  val table: Table = dynamoDB.getTable(tableName)

  def handleRequest(input: APIGatewayProxyRequestEvent, context: Context): ApiGatewayResponse = {
    val headers = Map("x-custom-response-header" -> "my custom response header value")
    //    val in = scalaMapper.writeValueAsString(input)

    val items: Array[Item] = scalaMapper
      .readValue(input.getBody, classOf[Array[java.util.Map[String, AnyRef]]])
      .map(Item.fromMap)
    context.getLogger.log(s"request: ${items.mkString("Array(", ", ", ")")}")
    val logger: Logger = LogManager.getLogger(getClass)
    logger.debug(s"request: {}", items)

    val writeItems = new TableWriteItems(tableName)
    val outcome: BatchWriteItemOutcome = dynamoDB.batchWriteItem(writeItems.withItemsToPut(items: _*))
    ApiGatewayResponse(200, outcome.getUnprocessedItems.toString, headers.asJava, base64Encoded = true)
  }
}
