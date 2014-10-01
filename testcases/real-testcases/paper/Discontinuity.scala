import leon.real._
import RealOps._

object Discontinuity {

  //Robustness Analysis of Finite Precision Implementations, arxiv extended version,
  // E. Goubault, S. Putot
  def simpleInterpolator(e: Real): Real = {
    require(0.0 <= e && e <= 100.0 && e +/- 0.00001)

    val r1: Real = 0.0
    val const: Real = 2.25  // else this would be pre-evaluated by the parser
    val r2: Real = 5 * const
    val const2: Real = 1.1
    val r3: Real = r2 + 20 * const2  //same here

    if (e < 5)
      e * 2.25 + r1
    else if (e < 25)
      (e - 5) * 1.1 + r2
    else
      r3
  } ensuring (res => -2.25e-5 <= res && res <= 33.26 && res +/- 3.5e-5)


  def squareRoot(i: Real): Real = {
    require(1.0 <= i && i <= 2.0 && i +/- 0.001)

    val sqrt2: Real = 1.414213538169860839843750

    if (i >= 2)
      sqrt2 * (1.0 + (i/2 - 1) * (0.5 - 0.125 * ( i/2 - 1)))
    else
      1 + (i - 1) * (0.5 + (i-1) * (-0.125 + (i - 1) * 0.0625))
  
  } ensuring (res => 1 <= res && res <= 1.4531 && res +/- 0.03941)


  def cubicSpline(x: Real): Real = {
    require(-2 <= x && x <= 2)

    if (x <= -1) {
      0.25 * (x + 2)* (x + 2)* (x + 2)
    } else if (x <= 0) {
      0.25*(-3*x*x*x - 6*x*x +4)
    } else if (x <= 1) {
      0.25*(3*x*x*x - 6*x*x +4)
    } else {
      0.25*(2 - x)*(2 - x)*(2 - x)
    }

  }  ensuring (res => res +/- 2.3e-8)


  def squareRoot3(x: Real): Real = {
    require(0 < x && x < 10 && x +/- 1e-10 )
    if (x < 1e-5) 1 + 0.5 * x
    else sqrt(1 + x)
  } ensuring( res => res +/- 1e-10) //valid

  def squareRoot3Invalid(x: Real): Real = {
    require(0 < x && x < 10 && x +/- 1e-10 )
    if (x < 1e-4) 1 + 0.5 * x
    else sqrt(1 + x)
  } ensuring( res => res +/- 1e-10) //invalid

 

  def linearFit(x: Real, y: Real): Real = {
    require(-4 <= x && x <= 4 && -4 <= y && y <= 4)

    if (x <= 0) {
      if (y <= 0) {
        0.0958099 - 0.0557219*x - 0.0557219*y
      } else {
        -0.0958099 + 0.0557219*x - 0.0557219*y
      }

    } else { // x >= 0
      if (y <= 0) {
        -0.0958099 - 0.0557219*x + 0.0557219*y
      } else {
        0.0958099 + 0.0557219*x + 0.0557219*y  
      }
    }

  } ensuring (res => res +/- 1e-5)


  //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  def quadraticFit(x: Real, y: Real): Real = {
    require(-4 <= x && x <= 4 && -4 <= y && y <= 4)

    if (x <= 0) {
      if (y <= 0) {
        -0.0495178 - 0.188656*x - 0.0502969*x*x - 0.188656*y + 0.0384002*x*y - 0.0502969*y*y
      } else {
        0.0495178 + 0.188656*x + 0.0502969*x*x - 0.188656*y + 0.0384002*x*y + 0.0502969*y*y
      }

    } else { // x >= 0
      if (y <= 0) {
        0.0495178 - 0.188656*x + 0.0502969*x*x + 0.188656*y + 0.0384002*x*y + 0.0502969*y*y
      } else {
        -0.0495178 + 0.188656*x - 0.0502969*x*x + 0.188656*y + 0.0384002*x*y - 0.0502969*y*y
      }
    }
  } ensuring (res => res +/- 1e-5)

  def quadraticFitErr(x: Real, y: Real): Real = {
    require(-4 <= x && x <= 4 && -4 <= y && y <= 4 && x +/- 0.1 && y +/- 0.1)
    
    if (x <= 0) {
      if (y <= 0) {
        -0.0495178 - 0.188656*x - 0.0502969*x*x - 0.188656*y + 0.0384002*x*y - 0.0502969*y*y
      } else {
        0.0495178 + 0.188656*x + 0.0502969*x*x - 0.188656*y + 0.0384002*x*y + 0.0502969*y*y
      }

    } else { // x >= 0
      if (y <= 0) {
        0.0495178 - 0.188656*x + 0.0502969*x*x + 0.188656*y + 0.0384002*x*y + 0.0502969*y*y
      } else {
        -0.0495178 + 0.188656*x - 0.0502969*x*x + 0.188656*y + 0.0384002*x*y - 0.0502969*y*y
      }
    }
  } ensuring (res => res +/- 1e-5)

  //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  def quadraticFit2(x: Real, y: Real): Real = {
    require(-4 <= x && x <= 4 && -4 <= y && y <= 4)

    if (x > y) {
      0.238604 - 0.143624*x + 0.0137*x*x + 0.143624*y + 0.00605411*x*y + 0.0137*y*y
    } else {
      0.238604 + 0.143624*x + 0.0137*x*x - 0.143624*y + 0.00605411*x*y + 0.0137*y*y
    }
  } ensuring (res => res +/- 1e-5)

  def quadraticFit2Err(x: Real, y: Real): Real = {
    require(-4 <= x && x <= 4 && -4 <= y && y <= 4 && x +/- 0.1 && y +/- 0.1)
    
    if (x > y) {
      0.238604 - 0.143624*x + 0.0137*x*x + 0.143624*y + 0.00605411*x*y + 0.0137*y*y
    } else {
      0.238604 + 0.143624*x + 0.0137*x*x - 0.143624*y + 0.00605411*x*y + 0.0137*y*y
    }
  } ensuring (res => res +/- 1e-5)

  //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  def styblinski(x: Real, y: Real): Real = {
    require(-5 <= x && x <= 5 && -5 <= y && y <= 5)
    
    if (y <= 0)
      if (x <= 0)
        4.43803 + 5.8375*x + 1.3875*x*x + 5.91829*y + 2.92311e-15*x*y + 1.36286*y*y 
      else 
        4.05 - 5.3375*x + 1.3875*x*x + 5.8375*y - 6.64404e-16*x*y + 1.3875*y*y   
    else 
      if (x <= 0)
        4.05 + 5.8375*x + 1.3875*x*x - 5.3375*y + 1.18116e-15*x*y + 1.3875*y*y   
      else 
        4.05 -5.3375*x + 1.3875*x*x - 5.3375*y - 7.38226e-17*x*y + 1.3875*y*y   
  } ensuring (res => res +/- 1e-5)


  def styblinskiErr(x: Real, y: Real): Real = {
    require(-5 <= x && x <= 5 && -5 <= y && y <= 5 && x +/- 0.1 && y +/- 0.1)
   
    if (y <= 0)
      if (x <= 0)
        4.43803 + 5.8375*x + 1.3875*x*x + 5.91829*y + 2.92311e-15*x*y + 1.36286*y*y 
      else 
        4.05 - 5.3375*x + 1.3875*x*x + 5.8375*y - 6.64404e-16*x*y + 1.3875*y*y   
    else 
      if (x <= 0)
        4.05 + 5.8375*x + 1.3875*x*x - 5.3375*y + 1.18116e-15*x*y + 1.3875*y*y   
      else 
        4.05 -5.3375*x + 1.3875*x*x - 5.3375*y - 7.38226e-17*x*y + 1.3875*y*y   
  } ensuring (res => res +/- 1e-5)


  //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  // this is the styblisnki function a little modified, y^3 instead of y^4
  def sortOfStyblinski(x: Real, y: Real): Real = {
    require(-5 <= x && x <= 5 && -5 <= y && y <= 5)

    if (y < x)
      -2.60357 + 0.25*x + 0.369643*x*x + 0.889286*y - 4.32143e-16*x*y - 0.896429*y*y  
    else
      -3.76071 + 0.25*x + 0.369643*x*x + 0.889286*y + 5.08864e-16*x*y - 0.703571*y*y  

  } ensuring (res => res +/- 1e-5)

  def sortOfStyblinskiErr(x: Real, y: Real): Real = {
    require(-5 <= x && x <= 5 && -5 <= y && y <= 5 && x +/- 0.1 && y +/- 0.1)
    
    if (y < x)
      -2.60357 + 0.25*x + 0.369643*x*x + 0.889286*y - 4.32143e-16*x*y - 0.896429*y*y  
    else
      -3.76071 + 0.25*x + 0.369643*x*x + 0.889286*y + 5.08864e-16*x*y - 0.703571*y*y  

  } ensuring (res => res +/- 1e-5)


  //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  def jetApprox(x: Real, y: Real): Real = {
    require(-5 <= x && x <= 5 && -5 <= y && y <= 5)

    if (y <= 0)
      if (x <= 0)
        0.31444 + 0.701312*x + 0.276567*x*x + 0.00810229*y + 0.00744214*x*y +
          0.00662727*x*x*y - 0.00332894*y*y - 0.000303936*x*y*y  
      else 
        0.296557 - 0.652413*x + 0.232566*x*x + 0.00379835*y - 0.000805898*x*y +
          0.0104647*x*x*y + 0.00332894*y*y - 0.000303936*x*y*y  
    else 
      if (x <= 0)
        0.31444 + 0.701312*x + 0.276567*x*x + 0.0178958*y + 0.0194342*x*y + 
          0.00902569*x*x*y - 0.00332894*y*y - 0.000303936*x*y*y 
      else 
        0.296557 - 0.652413*x + 0.232566*x*x - 0.0059952*y + 0.0111862*x*y + 
          0.00806632*x*x*y + 0.00332894*y*y - 0.000303936*x*y*y 

  }  ensuring (res => res +/- 1e-5)


  def jetApproxErr(x: Real, y: Real): Real = {
    require(-5 <= x && x <= 5 && -5 <= y && y <= 5 && x +/- 0.1 && y +/- 0.1)

    if (y <= 0)
      if (x <= 0)
        0.31444 + 0.701312*x + 0.276567*x*x + 0.00810229*y + 0.00744214*x*y +
          0.00662727*x*x*y - 0.00332894*y*y - 0.000303936*x*y*y 
      else 
        0.296557 - 0.652413*x + 0.232566*x*x + 0.00379835*y - 0.000805898*x*y +
          0.0104647*x*x*y + 0.00332894*y*y - 0.000303936*x*y*y  
    else 
      if (x <= 0)
        0.31444 + 0.701312*x + 0.276567*x*x + 0.0178958*y + 0.0194342*x*y + 
          0.00902569*x*x*y - 0.00332894*y*y - 0.000303936*x*y*y  
      else 
        0.296557 - 0.652413*x + 0.232566*x*x - 0.0059952*y + 0.0111862*x*y + 
          0.00806632*x*x*y + 0.00332894*y*y - 0.000303936*x*y*y  

  } ensuring (res => res +/- 1e-5)



  // maximum real difference: -0.212021
  def jetApproxGoodFit(x: Real, y: Real): Real = {
    require(-5 <= x && x <= 5 && -5 <= y && y <= 5 && x +/- 1e-11 && y +/- 1e-11)

    if (y < x)
      -0.367351 + 0.0947427*x + 0.0917402*x*x - 0.00298772*y + 0.0425403*x*y + 0.00204213*y*y 
    else
      -0.308522 + 0.0796111*x + 0.162905*x*x + 0.00469104*y - 0.0199035*x*y - 0.00204213*y*y  
  } ensuring (res => res +/- 1e-5)


  
  def jetApproxGoodFitErr(x: Real, y: Real): Real = {
    require(-5 <= x && x <= 5 && -5 <= y && y <= 5 && x +/- 0.1 && y +/- 0.1)

    if (y < x)
      -0.367351 + 0.0947427*x + 0.0917402*x*x - 0.00298772*y + 0.0425403*x*y + 0.00204213*y*y 
    else
      -0.308522 + 0.0796111*x + 0.162905*x*x + 0.00469104*y - 0.0199035*x*y - 0.00204213*y*y  
  } ensuring (res => res +/- 1e-5)


  // maximum real difference: -1.3571
  def jetApproxBadFit(x: Real, y: Real): Real = {
    require(-5 <= x && x <= 5 && -5 <= y && y <= 5)

    if (y < x)
      -0.156594 - 0.272509*x + 0.169103*x*x + 0.0890365*y - 0.0671758*x*y + 
        0.0214895*x*x*y + 0.0165351*y*y - 0.00402582*x*y*y 
    else
      -0.109552 + 0.35185*x + 0.218929*x*x - 0.15666*y - 0.123483*x*y - 
        0.0155622*x*x*y + 0.028652*y*y + 0.00852615*x*y*y  
  } ensuring (res => res +/- 1e-5)



  def jetApproxBadFitErr(x: Real, y: Real): Real = {
    require(-5 <= x && x <= 5 && -5 <= y && y <= 5 && x +/- 0.1 && y +/- 0.1)

    if (y < x)
      -0.156594 - 0.272509*x + 0.169103*x*x + 0.0890365*y - 0.0671758*x*y + 
        0.0214895*x*x*y + 0.0165351*y*y - 0.00402582*x*y*y 
    else
      -0.109552 + 0.35185*x + 0.218929*x*x - 0.15666*y - 0.123483*x*y - 
        0.0155622*x*x*y + 0.028652*y*y + 0.00852615*x*y*y  
  } ensuring (res => res +/- 1e-5)
}