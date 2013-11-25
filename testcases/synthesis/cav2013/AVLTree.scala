import scala.collection.immutable.Set
import leon.Annotations._
import leon.Utils._

object AVLTree {
  sealed abstract class Tree
  case class Node(left : Tree, value : Int, right : Tree) extends Tree
  case class Leaf() extends Tree

  def content(t : Tree): Set[Int] = t match {
    case Leaf() => Set.empty[Int]
    case Node(l, v, r) => content(l) ++ Set(v) ++ content(r)
  }

  def height(t: Tree): Int = t match {
    case Leaf() => 0
    case Node(l, _, r) =>
      val lh = height(l)
      val rh = height(r)
      if (rh > lh) {
        rh+1
      } else {
        lh+1
      }
  }

  def isSortedMinMax(t: Tree, min: Int, max: Int): Boolean = t match {
    case Node(l, v, r) =>
      isSortedMinMax(l, min, v) &&
      isSortedMinMax(r, v, max) &&
      v < max && v > min
    case _ => true
  }

  def isSortedMin(t: Tree, min: Int): Boolean = t match {
    case Node(l, v, r) =>
      isSortedMinMax(l, min, v) &&
      isSortedMin(r, v) &&
      v > min
    case _ => true
  }

  def isSortedMax(t: Tree, max: Int): Boolean = t match {
    case Node(l, v, r) =>
      isSortedMax(l, v) &&
      isSortedMinMax(r, v, max) &&
      v < max
    case _ => true
  }

  def isBalanced(t: Tree): Boolean = t match {
    case Node(l, v, r) =>
      val diff = height(l)-height(r)

      !(diff > 1 || diff < -1) && isBalanced(l) && isBalanced(r)
    case Leaf() =>
      true
  }

  def isSorted(t: Tree): Boolean = t match {
    case Node(l, v, r) =>
      isSortedMin(r, v) &&
      isSortedMax(l, v)
    case _ => true
  }

  def deleteSynth(in : Tree, v : Int) = choose {
    (out : Tree) => content(out) == (content(in) -- Set(v))
  }

  def insertSynth(in : Tree, v : Int) = choose {
    (out : Tree) => content(out) == (content(in) ++ Set(v))
  }

  def insertBalancedSynth(in: Tree, v: Int) = choose {
    (out : Tree) => isBalanced(in) && (content(out) == (content(in) ++ Set(v))) && isBalanced(out)
  }

  def insertSortedSynth(in : Tree, v : Int) = choose {
    (out : Tree) => isSorted(in) && (content(out) == (content(in) ++ Set(v))) && isSorted(out)
  }

  def deleteSortedSynth(in : Tree, v : Int) = choose {
    (out : Tree) => isSorted(in) && (content(out) == (content(in) -- Set(v))) && isSorted(out)
  }

  def deleteBalancedSynth(in: Tree, v: Int) = choose {
    (out : Tree) => isBalanced(in) && (content(out) == (content(in) -- Set(v))) && isBalanced(out)
  }
}
