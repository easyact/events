package cn.easyact.events

trait Event {
  val at: String
}

sealed trait EventRepoF[+A]

case class Store(e: Event) extends EventRepoF[Unit]

case class StoreJsonSeq(jsonArr: String) extends EventRepoF[Any]

case class Get(user: String) extends EventRepoF[List[Event]]