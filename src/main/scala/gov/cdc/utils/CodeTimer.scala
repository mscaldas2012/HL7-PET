package gov.cdc.utils

class CodeTimer { }

object CodeTimer {
    def time[R](block: => R): R = {
        val t0 = System.currentTimeMillis()
        val result = block    // call-by-name
        val t1 = System.currentTimeMillis()
        //Todo:: improve formatting here
        println(s"\nElapsed time:  ${t1 - t0} ms")
        result
    }
}
