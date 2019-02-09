package utils

/**
  * This class simply keeps printing a dot (.) on the console to let the user know
  * that the application still working.
  *
  * Created - 6/3/17
  * Author Marcelo Caldas mcq1@cdc.gov
  */
object ConsoleProgress {


    def showProgress[R](block: => R): R = {
        val thread = _startProgress()
        CodeTimer.time {
            val result = block
           thread.stop
            result
        }
    }


    //private var _thread: Thread = _
    private def _startProgress(): Thread = {
        val _thread = new Thread {
            override def run {
                while (true) {
                    print('.')
                    Thread.sleep(1000)
                }
            }
        }
        _thread.start
        _thread
    }


}

//object ConsoleProgress {}
