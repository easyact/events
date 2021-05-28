package cn.easyact.events

import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.document.{BatchWriteItemOutcome, Item, TableWriteItems}
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.fasterxml.jackson.databind.ObjectMapper

import java.time.Instant
import scala.jdk.CollectionConverters._
import scala.sys.env

class ApiGatewayHandler extends RequestHandler[APIGatewayProxyRequestEvent, ApiGatewayResponse] {

  import cn.easyact.events.ApiGatewayHandler._
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
    val log = context.getLogger
    log.log(s"env: $env")
    val items: Array[Item] = readArray(input.getBody).map(Item.fromJSON).map { i =>
      val email = i.getString("user.email")
      val at = Instant.now().toString
      val item = i.withPrimaryKey("user.email", email, "at", at)
      log.log(s"Mapping email: $email, item: $item, at: $at")
      item
    }
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

  private def readArray(input: String) = scalaMapper.readTree(input).iterator().asScala.toArray.map(_.toString)

  def main(args: Array[String]): Unit = {
    println(env)

    val json = """[{"t": {"S":"S", "B":true}},{"t":"S"}]"""
    val ss = readArray(json)
    println(s"2: ${ss.getClass}, ${ss.head.getClass}, ${ss.mkString("Array(", ", ", ")")}")
    println(s"3: ${ss.map(Item.fromJSON).mkString("Array(", ", ", ")")}")
  }
}
