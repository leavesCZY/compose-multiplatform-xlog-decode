package github.leavesczy.xlog.decode.platform

object DesktopOs {

    val isMacOs: Boolean =
        System.getProperty("os.name").orEmpty().equals("Mac OS X", ignoreCase = true)

}