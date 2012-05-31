/**
 * Created by IntelliJ IDEA.
 * User: soren
 * Date: 5/31/12
 * Time: 08:36
 * To change this template use File | Settings | File Templates.
 */

import org.specs2.mutable._

class HelloWorldSpec extends Specification {

  "The 'Hello world' string" should {
    "contain 11 characters" in {
      "Hello world" must have size(11)
    }
    "start with 'Hello'" in {
      "Hello world" must startWith("Hello")
    }
    "end with 'world'" in {
      "Hello world" must endWith("world")
    }
  }
}