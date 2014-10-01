import leon.real._
import RealOps._
import annotations._

object SineNewtonUnrolled {

  def newton(x: Real): Real = {
    require(-1.0 < x && x < 1.0 && -1.0 < ~x && ~x < 1.0)

    val x1 =  (x - ((((x - (((x * x) * x) / 6.)) + (((((x * x) * x) * x) * x) / 120.)) + 
      (((((((x * x) * x) * x) * x) * x) * x) / 5040.)) / (((1. - ((x * x) / 2.)) + ((((x * x) * x) * x) / 24.)) + 
      ((((((x * x) * x) * x) * x) * x) / 720.))))
    val  x2 =  (x1 - ((((x1 - (((x1 * x1) * x1) / 6.)) + (((((x1 * x1) * x1) * x1) * x1) / 120.)) + 
      (((((((x1 * x1) * x1) * x1) * x1) * x1) * x1) / 5040.)) / (((1. - ((x1 * x1) / 2.)) + ((((x1 * x1) * x1) * x1) / 24.)) + 
      ((((((x1 * x1) * x1) * x1) * x1) * x1) / 720.))))
    val  x3 =  (x2 - ((((x2 - (((x2 * x2) * x2) / 6.)) + (((((x2 * x2) * x2) * x2) * x2) / 120.)) + 
      (((((((x2 * x2) * x2) * x2) * x2) * x2) * x2) / 5040.)) / (((1. - ((x2 * x2) / 2.)) + ((((x2 * x2) * x2) * x2) / 24.)) +
       ((((((x2 * x2) * x2) * x2) * x2) * x2) / 720.))))
    val  x4 =  (x3 - ((((x3 - (((x3 * x3) * x3) / 6.)) + (((((x3 * x3) * x3) * x3) * x3) / 120.)) +
     (((((((x3 * x3) * x3) * x3) * x3) * x3) * x3) / 5040.)) / (((1. - ((x3 * x3) / 2.)) + ((((x3 * x3) * x3) * x3) / 24.)) +
      ((((((x3 * x3) * x3) * x3) * x3) * x3) / 720.))))
    val  x5 =  (x4 - ((((x4 - (((x4 * x4) * x4) / 6.)) + (((((x4 * x4) * x4) * x4) * x4) / 120.)) +
     (((((((x4 * x4) * x4) * x4) * x4) * x4) * x4) / 5040.)) / (((1. - ((x4 * x4) / 2.)) + ((((x4 * x4) * x4) * x4) / 24.)) +
      ((((((x4 * x4) * x4) * x4) * x4) * x4) / 720.))))
    val  x6 =  (x5 - ((((x5 - (((x5 * x5) * x5) / 6.)) + (((((x5 * x5) * x5) * x5) * x5) / 120.)) +
     (((((((x5 * x5) * x5) * x5) * x5) * x5) * x5) / 5040.)) / (((1. - ((x5 * x5) / 2.)) + ((((x5 * x5) * x5) * x5) / 24.)) +
      ((((((x5 * x5) * x5) * x5) * x5) * x5) / 720.))))
    val  x7 =  (x6 - ((((x6 - (((x6 * x6) * x6) / 6.)) + (((((x6 * x6) * x6) * x6) * x6) / 120.)) +
     (((((((x6 * x6) * x6) * x6) * x6) * x6) * x6) / 5040.)) / (((1. - ((x6 * x6) / 2.)) + ((((x6 * x6) * x6) * x6) / 24.)) +
      ((((((x6 * x6) * x6) * x6) * x6) * x6) / 720.))))
    val  x8 =  (x7 - ((((x7 - (((x7 * x7) * x7) / 6.)) + (((((x7 * x7) * x7) * x7) * x7) / 120.)) +
      (((((((x7 * x7) * x7) * x7) * x7) * x7) * x7) / 5040.)) / (((1. - ((x7 * x7) / 2.)) + ((((x7 * x7) * x7) * x7) / 24.)) +
       ((((((x7 * x7) * x7) * x7) * x7) * x7) / 720.))))
    val  x9 =  (x8 - ((((x8 - (((x8 * x8) * x8) / 6.)) + (((((x8 * x8) * x8) * x8) * x8) / 120.)) +
     (((((((x8 * x8) * x8) * x8) * x8) * x8) * x8) / 5040.)) / (((1. - ((x8 * x8) / 2.)) + ((((x8 * x8) * x8) * x8) / 24.)) +
      ((((((x8 * x8) * x8) * x8) * x8) * x8) / 720.))))
    
    ((((x9 - (((x9 * x9) * x9) / 6.)) + (((((x9 * x9) * x9) * x9) * x9) / 120.)) +
     (((((((x9 * x9) * x9) * x9) * x9) * x9) * x9) / 5040.)) / (((1. - ((x9 * x9) / 2.)) + ((((x9 * x9) * x9) * x9) / 24.)) +
      ((((((x9 * x9) * x9) * x9) * x9) * x9) / 720.)))

  } ensuring(res => -1.0 < res && res < 1.0 && -1.0 < ~res && ~res < 1.0)
}