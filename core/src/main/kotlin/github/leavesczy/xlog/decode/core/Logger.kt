package github.leavesczy.xlog.decode.core

/**
 * @Author: leavesCZY
 * @Date: 2024/6/4 14:17
 * @Desc:
 */
interface Logger {

    fun debug(log: () -> Any)

    fun error(log: () -> Any)

}