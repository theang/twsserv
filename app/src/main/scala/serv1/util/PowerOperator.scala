package serv1.util

import scala.math.pow

trait PowerOperator {
  implicit class PowerInt(i: Int) {
    def **(b: Int): Int = pow(i, b).intValue
  }
}
