package tm.toma

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by taro on 12/6/16.
 */

interface Loggable {
    val sLogger: Logger
        get() = LoggerFactory.getLogger(javaClass)
}