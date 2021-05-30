package cn.easyact.events

import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.document.{BatchWriteItemOutcome, DynamoDB, Item, PrimaryKey, TableWriteItems}
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder}
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.fasterxml.jackson.databind.ObjectMapper
import scalaz.concurrent.Task
import scalaz.concurrent.Task.now
import scalaz.~>

import java.time.Instant
import scala.collection.JavaConverters._
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

  private val table = dynamoDB.getTable(tableName)

  private val HASH_KEY = "user.email"

  private val RANGE_KEY = "at"

  def step: EventRepoF ~> Task = new (EventRepoF ~> Task) {
    override def apply[A](fa: EventRepoF[A]): Task[A] = fa match {
      case StoreJsonSeq(jsonArr) =>
        val items = toItems(jsonArr)
        log.log(s"request: ${items.mkString("Array(", ", ", ")")}")
        val outcome: BatchWriteItemOutcome = dynamoDB.batchWriteItem(
          new TableWriteItems(tableName).withItemsToPut(items: _*))
        now(outcome.getUnprocessedItems)
      case Get(user) =>
        val outcomes = queryBy(user)
        log.log(s"Get events of $user are: $outcomes")
        now(outcomes.asScala.map(_.asMap()).toSeq)
      case Delete(user) =>
        val keys = queryBy(user).asScala.map(i => new PrimaryKey(HASH_KEY, user, RANGE_KEY, i.getString(RANGE_KEY)))
        val items = new TableWriteItems(tableName).withPrimaryKeysToDelete(keys.toSeq: _*)
        val outcome = dynamoDB.batchWriteItem(items)
        now(outcome)
      case Store(event) => now(table.putItem(toItem(event)))
    }
  }

  private def queryBy[A](user: String) = {
    table.query(HASH_KEY, user)
  }

  private def toItems(jsonArr: String): Array[Item] = jsonToStrings(jsonArr).map(toItem)

  private def toItem(s: String) = {
    val i = Item.fromJSON(s)
    val email: String = i.getMap("user").get("email")
    val at = Instant.now().toString
    val item = i.withPrimaryKey(HASH_KEY, email, RANGE_KEY, at)
    log.log(s"Mapping email: $email, item: $item, $RANGE_KEY: $RANGE_KEY")
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
