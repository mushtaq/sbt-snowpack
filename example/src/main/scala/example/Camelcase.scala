package example

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@JSImport("https://cdn.skypack.dev/camelcase@^6.0.0", JSImport.Default)
@js.native
object Camelcase extends js.Object {
  def apply(input: String): String = js.native
}
