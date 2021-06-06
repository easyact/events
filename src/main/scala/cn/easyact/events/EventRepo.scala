package cn.easyact.events

import java.util

trait Event {
  val at: String
}

sealed trait EventRepoF[+A]

case class Store(user: String, at: String, event: String) extends EventRepoF[Any]

case class StoreJsonSeq(jsonArr: Array[String]) extends EventRepoF[Any]

case class Get(user: String, beginAt: Option[String] = None) extends EventRepoF[Seq[util.Map[String, AnyRef]]]

case class AllAt(user: String, at: String) extends EventRepoF[Any]

case class Delete(user: String) extends EventRepoF[Any]