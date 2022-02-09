package example

import scala.concurrent.ExecutionContext.Implicits.global

object Main {

  def main(args: Array[String]): Unit = {
    println(Utils.camelcase("my name is ABC blah hah"))
    Utils.printEachAndCollect(5).onComplete(println)
  }

}
