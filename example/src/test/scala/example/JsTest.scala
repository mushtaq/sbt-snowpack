package example

import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.async.Async._
import scala.concurrent.ExecutionContext

class JsTest extends AsyncFreeSpec with Matchers {

  override implicit def executionContext: ExecutionContext = ExecutionContext.global

  "rxjs" in async {
    println("starting test *********************")
    await(Utils.printEachAndCollect(5)) shouldBe (0 to 4)
  }

  "camelcase" in async {
    println("starting test *********************")
    Utils.camelcase("my name is blah") shouldBe "myNameIsBlah"
  }

}
