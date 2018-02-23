package io.github.karino2.filescripting

import bsh.Interpreter
import java.io.File
import bsh.StringUtil
import kotlin.coroutines.experimental.buildSequence


/**
 * Created by _ on 2018/02/23.
 * Facade for script command
 */
class Builtins(val intp : Interpreter, val ctx: MainActivity) {
    fun ls(dir: Any?): Iterable<File> {
        when(dir) {
            is String -> {
                return lsFile(intp.pathToFile(dir))
            }
            is File -> {
                return lsFile(dir)
            }
        }
        return listOf<File>()
    }

    fun lsFile(target: File) : Iterable<File> {
        if(!target.exists() || !target.canRead()) {
            ctx.perrorln("Can't read " + target.toString())
            return listOf<File>()
        }
        if(!target.isDirectory) {
            return listOf<File>(target)
        }

        var files = target.list()
        files = StringUtil.bubbleSort(files)

        val seq = buildSequence {
            for(file in files) {
                yield(File (target, file))
            }

        }
        return seq.asIterable()
    }
}