package example

import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, Future}

class JsTest extends AsyncFreeSpec with Matchers {

  override implicit def executionContext: ExecutionContext = ExecutionContext.global

  "rxjs" in Future.unit.flatMap { _ =>
    println("starting test *********************")
    Utils.printEachAndCollect(5).map { xs =>
      xs shouldBe (0 to 4)
    }
  }

  "camelcase" in {
    println("starting test *********************")
    Utils.camelcase("my name is blah") shouldBe "myNameIsBlah"
  }

}
