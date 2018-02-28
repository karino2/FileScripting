package io.github.karino2.filescripting

import java.util.*

/**
 * Created by _ on 2018/02/27.
 */
data class ScriptModel(var id : Long = -1, var script : String = "", var lastModified : Long = Date().time) {
}