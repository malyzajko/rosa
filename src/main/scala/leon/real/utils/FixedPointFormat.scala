/* Copyright 2009-2015 EPFL, Lausanne */

package leon
package real

import Rational._
//import AffineUtils._
import collection.mutable.Queue

object FixedPointFormat {

  /** Default rounding mode. */
  var globalRounding = false
  /** Default setting for whether to allow unsigned format .*/
  var allowUnsignedFormat = false

  def apply(b: Int, frac: Int): FixedPointFormat = FixedPointFormat(true, b, frac, false)

  /**
    Computes the best format for an interval of values while avoiding overflow.
    Will return an unsigned format, if possible and allowed and uses default rounding.
    Throws an FixedPointOverflowException, if the range cannot fit in the given bits,
    and overflow checking is enabled.
    @param a lower bound of range of values
    @param b upper bound of range of values
    @param bits bitvector length
   */
  def getFormat(a: Rational, b: Rational, bits: Int): FixedPointFormat = {
    assert(a <= b,
      "Range given must have lower bound (%s) smaller than upper bound (%s)".format(a.toString, b.toString))
    val signed = if (allowUnsignedFormat) a < Rational(0) else true
    val intBits = bitsNeeded(math.max(math.abs(a.integerPart), math.abs(b.integerPart)))
    // We don't allow 0 fractional bits
    if (intBits >= bits) {
      throw FixedPointOverflowException("Number of max bits (%d) exceeded: %d".format(bits, intBits))
    }
    val fracBits = if (signed) bits - intBits - 1 else bits - intBits
    new FixedPointFormat(signed, bits, fracBits, globalRounding)
  }

  def getFormat(i: RationalInterval, bits: Int): FixedPointFormat = {
    val prelim = getFormat(i.xlo, i.xhi, bits)
    val secondary = getFormat(i.xlo - prelim.quantError, i.xhi + prelim.quantError, bits)
    if (prelim != secondary) secondary
    else prelim
  }

  /**
    Computes the best format needed to represent a rational number (i.e. a constant).
    The result is always a signed format.
   @param r rational to be represented
   @param bitLength length of the bitvector that's available
   */
  def getFormat(r: Rational, bitLength: Int): FixedPointFormat = {
    val fracBits = fractionalBits(math.abs(r.toDouble), bitLength)
    val tmp = new FixedPointFormat(true, bitLength, fracBits, globalRounding)
    tmp
  }

  private def fractionalBits(constant: Double, totalBits: Int): Int = {
    math.ceil(totalBits - 2 - math.log(constant)/math.log(2)).toInt
  }

  /**
   Returns the number of bits needed to represent the given integer.
   Assumes 32-bit integers.
   */
  private def bitsNeeded(value: Int): Int = {
    32 - Integer.numberOfLeadingZeros(value)
  }

  // Conversion functions from rational to fixedpoint.
    // The long version has to fit into 32 bit int, hence we can do this funny conversion.
    //@param f # fractional bits
  def doubleToLong(d: Double, f: Int): Long = (d * math.pow(2, f)).round.toLong
  def longToDouble(i: Long, f: Int): Double = i.toDouble / math.pow(2, f)
  def rationalToLong(r: Rational, f: Int): Long = (r * Rational(math.pow(2, f))).roundToLong
  def longToRational(i: Long, f: Int): Rational = Rational(i) / Rational(math.pow(2, f))
  def rationalToInt(r: Rational, f: Int): Int = (r * Rational(math.pow(2, f))).roundToInt

}

/**
 * A fixed-point format.
 * @param signed sign bit, if true the fixed-point numbers are signed
 * @param bits bitvector length
 * @param f number of fractional bits
 * @param realRounding true if true rounding is used, truncation otherwise
 */
case class FixedPointFormat(val signed: Boolean, val bits: Int, val f: Int, val realRounding: Boolean) {
  val weight = math.pow(2, f).asInstanceOf[Long] // denominator of weight really

  assert(signed, "Unsigned format!")


  /**
    Number of integer bits. Can be zero for numbers smaller than 1.
   */
  val i = math.max(if (signed) bits - f - 1 else bits - f, 0)

  assert(i >= 0, "number of integer bits is negative: %d!".format(i))
  assert(f >= 0, "number of fractional bits is negative: %d!".format(f))

  /**
    The range of this fixedpoint format.
   */
  val range: (Rational, Rational) = {
    if (signed) {
      ( Rational(-math.pow(2, bits-1).asInstanceOf[Long], weight),
        Rational(math.pow(2, bits-1).asInstanceOf[Long] - 1, weight))
    }
    else {
      (Rational(0, 1), Rational(math.pow(2, bits).asInstanceOf[Long] - 1, weight))
    }
  }

  /**
    Determines whether this format includes a certain range.
    @param interval range to test
   */
  def includesRange(interval: (Rational, Rational)): Boolean = {
    assert(interval._1 <= interval._2)
    range._1 <= interval._1 && range._2 >= interval._2
  }

  /**
    Checks if a given number (constant) is representable in this format.
    @param number to check
  */
  def canRepresent(d: Rational): Boolean = {
    val tmp = d.toDouble * math.pow(2, f)
    (tmp.toInt == tmp)
  }

  /**
    Quantization error of this format.
   */
  val quantError: Rational =
    if (realRounding) Rational(1, math.pow(2, (f + 1)).asInstanceOf[Long])
    else Rational(1, math.pow(2, f).asInstanceOf[Long])

  override def toString =
    "<%d,%d,%d>".format( if (signed) 1 else 0, bits, f)

}
