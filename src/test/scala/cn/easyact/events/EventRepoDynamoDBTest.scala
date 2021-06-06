package cn.easyact.events

import org.scalatest._
import flatspec._
import matchers._

class EventRepoDynamoDBTest extends AnyFlatSpec with should.Matchers {
  "Jackson" should "parse nullable field" in {
    val o = ApiGatewayHandler.readUnUploadedCommands(
      s"""
         |{
         |  "commands": [{
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
         |  }]
         |}""".stripMargin)
    o.commands.length shouldBe 1
    o.beginAt shouldBe None
  }
}
