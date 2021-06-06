package cn.easyact.events

import cn.easyact.events.ApiGatewayHandler.jsonToStrings
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec
import com.amazonaws.services.dynamodbv2.document.{BatchWriteItemOutcome, DynamoDB, Item, ItemCollection, PrimaryKey, QueryOutcome, RangeKeyCondition, TableWriteItems}
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder}
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.fasterxml.jackson.databind.ObjectMapper
import scalaz.concurrent.Task
import scalaz.concurrent.Task.now
import scalaz.~>
import scalaz.Scalaz._

import java.time.{Duration, Instant, OffsetDateTime}
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

  private val table = dynamoDB.getTable(tableName)

  private val HASH_KEY = "user.id"

  private val RANGE_KEY = "at"

  def step: EventRepoF ~> Task = new (EventRepoF ~> Task) {

    override def apply[A](fa: EventRepoF[A]): Task[A] = fa match {
      case StoreJsonSeq(jsonArr) =>
        val items = jsonArr.map(toItem(_))
        log.log(s"request: ${items.mkString("Array(", ", ", ")")}")
        if (items.nonEmpty) {
          val writeItems = new TableWriteItems(tableName).withItemsToPut(items: _*)
          val outcome: BatchWriteItemOutcome = dynamoDB.batchWriteItem(writeItems)
          now(outcome.getUnprocessedItems)
        } else {
          log.log(s"No op because empty events")
          now(Map())
        }
      case Get(user, beginAt) =>
        val outcomes = queryBy(user, beginAt)
        log.log(s"Get events of $user are: $outcomes")
        now(outcomes.asScala.map(_.asMap()).toSeq)
      case Delete(user) => update(user)
      case Store(user, at, event) => now(table.putItem(toItem(event, user.some, at)))
      case AllAt(user, beginAt) =>
        import java.time.OffsetDateTime.parse
        val beginTime = parse(beginAt)
        val ops = (items: Iterable[Item]) => items.lastOption
          .map(_.getString(RANGE_KEY)).map(parse).map(Duration.between(beginTime, _))
          .map { d =>
            items.map(i => {
              val newAt = parse(i.getString(RANGE_KEY)).minus(d).toString
              i.withPrimaryKey(i.getString(HASH_KEY), newAt)
            }).foldLeft(deleteItems(items))(_.withItemsToPut(_))
          }.get
        update(user, ops)
    }
  }

  private val deleteItems = (items: Iterable[Item]) => {
    val keys = items.map(i => new PrimaryKey(HASH_KEY, i.getString(HASH_KEY), RANGE_KEY, i.getString(RANGE_KEY)))
    new TableWriteItems(tableName).withPrimaryKeysToDelete(keys.toSeq: _*)
  }

  private def update[A](user: String, ops: Iterable[Item] => TableWriteItems = deleteItems): Task[A] = {
    val items: Iterable[Item] = queryBy(user).asScala
    if (items.isEmpty) {
      log.log(s"No op because empty events")
      now(Map().asInstanceOf[A])
    } else {
      val outcome = dynamoDB.batchWriteItem(ops(items))
      now(outcome.asInstanceOf[A])
    }
  }

  def queryBy(user: String, beginAt: Option[String] = None): ItemCollection[QueryOutcome] = beginAt
    .fold(table.query(HASH_KEY, user)) { at: String =>
      val spec = new QuerySpec().withHashKey(HASH_KEY, user)
        .withRangeKeyCondition(new RangeKeyCondition("at").gt(at))
      table.query(spec)
    }

  private def toItem(s: String, user: Option[String] = None, at: String = Instant.now().toString) = {
    val i = Item.fromJSON(s)
    val uid: String = user.getOrElse(i.getMap("user").get("id"))
    val item = i.withPrimaryKey(HASH_KEY, uid, RANGE_KEY, at)
    log.log(s"Mapping uid: $uid, item: $item, $RANGE_KEY: $RANGE_KEY")
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
