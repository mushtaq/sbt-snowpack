package example

import scala.concurrent.ExecutionContext.Implicits.global

object Main {

  def main(args: Array[String]): Unit = {
    println(Utils.camelcase("my name is blah"))
    Utils.printEachAndCollect(5).onComplete(println)
  }

}
