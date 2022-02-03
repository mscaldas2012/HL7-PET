package utils

import scala.language.implicitConversions

object IntUtils {
  implicit class SafeInt(val nbr: String) {
    def safeToInt(default: Int = 0): Int = {
      try {
        nbr.toInt
      } catch {
        case _: NumberFormatException => default
      }
    }
  }
}
