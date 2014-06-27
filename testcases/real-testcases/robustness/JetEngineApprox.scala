import leon.real._
import RealOps._
import annotations._

object JetEngineApprox {

  //we scaled the jet engine by 1000

  def jetApprox(x: Real, y: Real): Real = {
    require(-5 <= x && x <= 5 && -5 <= y && y <= 5)// && x +/- 1e-8 && y +/- 1e-8)

    if (y <= 0)
      if (x <= 0)
        0.31444 + 0.701312*x + 0.276567*x*x + 0.00810229*y + 0.00744214*x*y +
          0.00662727*x*x*y - 0.00332894*y*y - 0.000303936*x*y*y   // x<=0&&y<=0        
      else 
        0.296557 - 0.652413*x + 0.232566*x*x + 0.00379835*y - 0.000805898*x*y +
          0.0104647*x*x*y + 0.00332894*y*y - 0.000303936*x*y*y  //  x>=0&&y<=0        
    else 
      if (x <= 0)
        0.31444 + 0.701312*x + 0.276567*x*x + 0.0178958*y + 0.0194342*x*y + 
          0.00902569*x*x*y - 0.00332894*y*y - 0.000303936*x*y*y  //  x<=0&&y>=0        
      else 
        0.296557 - 0.652413*x + 0.232566*x*x - 0.0059952*y + 0.0111862*x*y + 
          0.00806632*x*x*y + 0.00332894*y*y - 0.000303936*x*y*y   // x>=0&&y>=0

  }






  def jetApproxGoodFit(x: Real, y: Real): Real = {
    require(-5 <= x && x <= 5 && -5 <= y && y <= 5)// && x +/- 1e-8 && y +/- 1e-8)

    if (y < x)
      -0.367351 + 0.0947427*x + 0.0917402*x*x - 0.00298772*y + 0.0425403*x*y + 0.00204213*y*y  //  y<x
    else
      -0.308522 + 0.0796111*x + 0.162905*x*x + 0.00469104*y - 0.0199035*x*y - 0.00204213*y*y   // x<y
  }


  def jetApproxBadFit(x: Real, y: Real): Real = {
    require(-5 <= x && x <= 5 && -5 <= y && y <= 5)// && x +/- 1e-8 && y +/- 1e-8)

    if (y < x)
      -0.156594 - 0.272509*x + 0.169103*x*x + 0.0890365*y - 0.0671758*x*y + 
        0.0214895*x*x*y + 0.0165351*y*y - 0.00402582*x*y*y //  y<x      
    else
      -0.109552 + 0.35185*x + 0.218929*x*x - 0.15666*y - 0.123483*x*y - 
        0.0155622*x*x*y + 0.028652*y*y + 0.00852615*x*y*y  // x<y      
  }




}