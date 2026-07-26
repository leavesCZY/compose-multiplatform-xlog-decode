package github.leavesczy.xlog.decode.core

interface Logger {

    fun debug(message: () -> String)

    fun error(message: () -> String)

}