package blaze.pipeline
package stages

import org.scalatest.{Matchers, WordSpec}
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable.ListBuffer

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * @author Bryce Anderson
 *         Created on 1/7/14
 */
class SerializingStageSpec extends WordSpec with Matchers {

  class SlowIntHead extends SlowHead[Int] {

    val ints = new ListBuffer[Int]

    val i = new AtomicInteger(0)
    def get: Int = i.getAndIncrement

    def write(data: Int): Unit = {
      ints += data
    }

    def name: String = "SlowIntHead"
  }

  class Nameless extends TailStage[Int] {
    def name: String = "int getter"
  }

  "SerializingStage" should {

    val tail = new Nameless
    val head = new SlowIntHead

    // build our pipeline
    LeafBuilder(tail).prepend(new SerializingStage[Int]).base(head)

    val ints = (0 until 200).toList

    "serialize reads" in {
      val tail = new Nameless
      val head = new SlowIntHead

      // build our pipeline
      LeafBuilder(tail).prepend(new SerializingStage[Int]).base(head)


      val results = ints map { i =>
        tail.channelRead()
      }

      val numbers = Future.sequence(results)
      Await.result(numbers, 10.seconds) should equal(ints)
    }

    "serialize writes" in {
      val f = 0 until 200 map { i =>
        tail.channelWrite(i)
      } last

      Await.result(f, 20.seconds)
      head.ints.result() should equal(ints)
    }
  }

}
