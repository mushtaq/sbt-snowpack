package example

import typings.rxjs.{mod => rxjs, rxjsMod => ops}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Utils {
  def printEachAndCollect(n: Int): Future[List[Double]] = {
    val observable = rxjs.interval(1000).pipe(ops.take(10))
    observable.subscribe(x => println(x))
    val result     = observable.pipe(ops.take(n), ops.toArray[Double]())
    result.toPromise[js.Array[Double]]().toFuture.map(_.toList)
  }

  def camelcase(input: String): String = camelCaseHelper(input)

  @JSImport("https://cdn.skypack.dev/camelcase@^6.0.0", JSImport.Default)
  @js.native
  private def camelCaseHelper(input: String): String = js.native
}
