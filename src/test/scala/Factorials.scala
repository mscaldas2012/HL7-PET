class Factorials {

    //@scala.annotation.tailrec
     def factSeq(lim: Int, cur: Int = 1, xs: List[Long] = List(1L, 1L)): List[Long] =
        if (cur > lim)
            xs.reverse
        else
            factSeq(lim, cur +1, xs(1) + xs.head :: xs)
}

object FactApp extends App {
    val f = new Factorials
    println(f.factSeq(8))
}
