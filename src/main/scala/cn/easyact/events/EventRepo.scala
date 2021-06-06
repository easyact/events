package cn.easyact.events

import java.util

trait Event {
  val at: String
}

sealed trait EventRepoF[+A]

case class Store(event: String) extends EventRepoF[Any]

case class StoreJsonSeq(jsonArr: Array[String]) extends EventRepoF[Any]

case class Get(user: String, beginAt: Option[String] = None) extends EventRepoF[Seq[util.Map[String, AnyRef]]]

//case class Sync(user: String, jsonArr: String, beginAt: Option[String] = None) extends EventRepoF[Seq[util.Map[String, AnyRef]]]

case class Delete(user: String) extends EventRepoF[Any]