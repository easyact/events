package cn.easyact.events

trait Event {
  val at: String
}

sealed trait EventRepoF[+A]

case class Store(event: String) extends EventRepoF[Any]

case class StoreJsonSeq(jsonArr: String) extends EventRepoF[Any]

case class GetEvents(user: String) extends EventRepoF[List[Event]]