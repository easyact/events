package cn.easyact.events

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.document.{BatchWriteItemOutcome, Item, TableWriteItems}
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.fasterxml.jackson.databind.ObjectMapper

import java.util
import scala.jdk.CollectionConverters._
import scala.sys.env

class ApiGatewayHandler extends RequestHandler[APIGatewayProxyRequestEvent, ApiGatewayResponse] {

  import cn.easyact.events.ApiGatewayHandler._
  import com.amazonaws.client.builder.AwsClientBuilder
  import com.amazonaws.services.dynamodbv2.document.{DynamoDB, Table}
  import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder}

  //  val client: AmazonDynamoDB = AmazonDynamoDBClientBuilder.standard.withEndpointConfiguration(
  //    new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-west-2")
  //  ).build
  val client: AmazonDynamoDB = AmazonDynamoDBClientBuilder.standard.withRegion(Regions.CN_NORTHWEST_1).build

  val dynamoDB = new DynamoDB(client)

  private val tableName: String = env.getOrElse("events_table", "events")
  val table: Table = dynamoDB.getTable(tableName)

  def handleRequest(input: APIGatewayProxyRequestEvent, context: Context): ApiGatewayResponse = {
    val items: Array[Item] = readArray(input.getBody).map(Item.fromMap)
    val log = context.getLogger
    log.log(s"env: $env")
    log.log(s"request: ${items.mkString("Array(", ", ", ")")}")
    val outcome: BatchWriteItemOutcome = dynamoDB.batchWriteItem(
      new TableWriteItems(tableName).withItemsToPut(items: _*))
    ApiGatewayResponse(200, outcome.getUnprocessedItems.toString,
      Map("x-custom-response-header" -> "my custom response header value").asJava,
      base64Encoded = true)
  }
}

object ApiGatewayHandler {
  val scalaMapper: ObjectMapper = {
    import com.fasterxml.jackson.databind.ObjectMapper
    import com.fasterxml.jackson.module.scala.DefaultScalaModule
    new ObjectMapper().registerModule(new DefaultScalaModule)
  }
  val javaMapper: ObjectMapper = new ObjectMapper()

  private def readArray(input: String) = javaMapper
    .readValue(input, classOf[Array[util.Map[String, AnyRef]]])

  def main(args: Array[String]): Unit = {
    println(env)

    val maps: Array[util.Map[String, AnyRef]] = readArray("""[{"t": {"S":"S", "B":true}},{"t":"S"}]""")
    val items = maps.map(Item.fromMap)
    println(s"${maps.getClass}, ${maps(0).getClass}, ${items.mkString("Array(", ", ", ")")}")
  }
}
