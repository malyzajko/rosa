import leon.Real
import Real._

// Tests if-then-else handling
// including path error
object CAV10_1 {

  def cav10(x: Real): Real = {
    require(x >< (0, 10))
    if (x*x - x >= 0)
      x/10
    else 
      x*x + 2
  } ensuring(res => 0 <= res && res <= 3.0)

}