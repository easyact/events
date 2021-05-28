package cn.easyact.events

import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.document.{BatchWriteItemOutcome, DynamoDB, Item, TableWriteItems}
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder}
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.fasterxml.jackson.databind.ObjectMapper
import scalaz.concurrent.Task
import scalaz.concurrent.Task.now
import scalaz.~>

import java.time.Instant
import scala.jdk.CollectionConverters._
import scala.sys.env

trait EventRepoInterpreter {
  def apply[A](action: EventRepo[A]): Task[A]
}

case class EventRepoDynamoDB(log: LambdaLogger) extends EventRepoInterpreter {
  //  val client: AmazonDynamoDB = AmazonDynamoDBClientBuilder.standard.withEndpointConfiguration(
  //    new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-west-2")
  //  ).build
  val client: AmazonDynamoDB = AmazonDynamoDBClientBuilder.standard.withRegion(Regions.CN_NORTHWEST_1).build

  val dynamoDB = new DynamoDB(client)

  private val tableName: String = env.getOrElse("events_table", "events")

  val scalaMapper: ObjectMapper = {
    import com.fasterxml.jackson.databind.ObjectMapper
    import com.fasterxml.jackson.module.scala.DefaultScalaModule
    new ObjectMapper().registerModule(new DefaultScalaModule)
  }

  private def jsonToStrings(input: String) = scalaMapper.readTree(input).iterator().asScala.toArray.map(_.toString)

  def step: EventRepoF ~> Task = new (EventRepoF ~> Task) {
    override def apply[A](fa: EventRepoF[A]): Task[A] = fa match {
      case StoreJsonSeq(jsonArr) =>
        val items = toItems(jsonArr)
        log.log(s"request: ${items.mkString("Array(", ", ", ")")}")
        val outcome: BatchWriteItemOutcome = dynamoDB.batchWriteItem(
          new TableWriteItems(tableName).withItemsToPut(items: _*))
        now(outcome)
      case Get(user) => now(List(user))
      case Store(event) => now(dynamoDB.getTable(tableName).putItem(toItem(event)))
    }
  }

  private def toItems(jsonArr: String): Array[Item] = jsonToStrings(jsonArr).map(toItem)

  private def toItem(s: String) = {
    val i = Item.fromJSON(s)
    val email: String = i.getMap("user").get("email")
    val at = Instant.now().toString
    val item = i.withPrimaryKey("user.email", email, "at", at)
    log.log(s"Mapping email: $email, item: $item, at: $at")
    item
  }

  override def apply[A](action: EventRepo[A]): Task[A] = action.foldMap(step)

}

object EventRepoDynamoDB {
  private val eventRepoDynamoDB = EventRepoDynamoDB(new LambdaLogger {
    override def log(message: String): Unit = println(message)

    override def log(message: Array[Byte]): Unit = println(message.mkString("Array(", ", ", ")"))
  })

  import eventRepoDynamoDB._

  def main(args: Array[String]): Unit = {
    println(env)

    val json = """[{"t": {"S":"S", "B":true}},{"t":"S"}]"""
    val ss = jsonToStrings(json)
    println(s"2: ${ss.getClass}, ${ss.head.getClass}, ${ss.mkString("Array(", ", ", ")")}")
    println(s"3: ${ss.map(Item.fromJSON).mkString("Array(", ", ", ")")}")
  }

}
