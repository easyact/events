package cn.easyact.events

import cn.easyact.events.EventRepoDynamoDB.toItem
import org.scalatest._
import flatspec._
import matchers._

class EventRepoDynamoDBTest extends AnyFlatSpec with should.Matchers {
  private val event =
    s"""  {
       |    "type": "IMPORT_BUDGET",
       |    "user": {
       |      "id": "damoco@easyact.cn"
       |    },
       |    "to": {
       |      "version": "0"
       |    },
       |    "payload": {
       |      "assets": [
       |        {
       |          "id": "1",
       |          "name": "food",
       |          "amount": 10
       |        }
       |      ]
       |    }
       |  }""".stripMargin
  private val events =
    s"""
       |{
       |  "commands": [$event]
       |}""".stripMargin
  "Jackson" should "parse nullable field" in {
    val o = ApiGatewayHandler.readUnUploadedCommands(events)
    o.commands.length shouldBe 1
    o.beginAt shouldBe None
  }

  "toItem" should "work" in {
    val item = toItem(event)
    item.asMap().get("user") shouldNot be(null)
  }

  "Option" should "work" in {
    None.getOrElse("a") shouldBe "a"
  }
}
