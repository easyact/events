package cn.easyact.events

trait Event {
  val at: String
}

sealed trait EventRepoF[+A]

case class Store(e: Event) extends EventRepoF[Unit]

case class StoreJsonSeq() extends EventRepoF[Unit]