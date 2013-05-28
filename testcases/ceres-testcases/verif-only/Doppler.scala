
import leon.Real
import Real._

/**
  Equation and initial ranges from:
  A.B. Kinsman and N. Nicolici. Finite Precision bit-width allocation using
  SAT-Modulo Theory. In DATE, 2009.
 */
object Doppler {

  def doppler(u: Real, v: Real, T: Real): Real = {
    require(-100 <= u && u <= 100 && 20 <= v && v <= 20000 &&
     -30 <= T && T <= 50)
    (- (331.4 + 0.6 * T) *v) / ((331.4 + 0.6*T + u)*(331.4 + 0.6*T + u))
  } ensuring (res => 0.0 <= res && res <= 138.0)
  // TODO: fix the postcondition 
  
  def doppler2(u: Real, v: Real, T: Real): Real = {
    require(-125 <= u && u <= 125 && 15 <= v && v <= 25000 &&
     -40 <= T && T <= 60)
    (- (331.4 + 0.6 * T) *v) / ((331.4 + 0.6*T + u)*(331.4 + 0.6*T + u))
  } ensuring (res => 0.0 <= res && res <= 138.0)

  def doppler3(u: Real, v: Real, T: Real): Real = {
    require(-30 <= u && u <= 120 && 320 <= v && v <= 20300 &&
     -50 <= T && T <= 30)
    (- (331.4 + 0.6 * T) *v) / ((331.4 + 0.6*T + u)*(331.4 + 0.6*T + u))
  } ensuring (res => 0.0 <= res && res <= 138.0)


}
