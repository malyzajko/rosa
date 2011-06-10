import cp.Terms._
import cp.Definitions._

object LazyVars {
  def NonnegativeInt = ((x: Int) => x >= 0).lazyFindAll
  def chooseInt(lower: Int, upper: Int) = ((x: Int) => x >= lower && x <= upper).lazyFindAll

  def main(args: Array[String]): Unit = {
    for {
      x <- chooseInt(0, 5)
      y <- chooseInt(3, 6)
      if y < x
    } {
      println(((a: Int) => a > x).solve)
      val i: Int = x
      val j: Int = y
      println(i, j)
    }

    /*
    println("...")

    for {
      x <- NonnegativeInt
      y <- NonnegativeInt // replace these with Stream.from(0) and the code will loop forever without printing anything
      if x > y && x < 10
    } {
      val i: Int = x
      val j: Int = y
      println(i, j)
    }
    */
  }
}