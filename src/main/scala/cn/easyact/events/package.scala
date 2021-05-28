package cn.easyact

import scalaz.Free

package object events {
  type EventRepo[A] = Free[EventRepoF, A]
}
