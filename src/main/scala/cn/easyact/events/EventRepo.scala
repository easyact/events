package cn.easyact.events

import java.util

trait Event {
  val at: String
}

sealed trait EventRepoF[+A]

case class Store(event: String) extends EventRepoF[Any]

case class StoreJsonSeq(jsonArr: String) extends EventRepoF[Any]

case class Get(user: String) extends EventRepoF[Seq[util.Map[String, AnyRef]]]

case class Delete(user: String) extends EventRepoF[Any]