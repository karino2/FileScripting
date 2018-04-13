package io.github.karino2.filescripting

import bsh.Interpreter
import java.io.File
import bsh.StringUtil
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.IOException
import kotlin.coroutines.experimental.buildSequence


/**
 * Created by _ on 2018/02/23.
 * Facade for script command
 */
class Builtins(val intp : Interpreter, val ctx: MainActivity) {
    val String.isPattern
    get()= this.contains("*")

    val Interpreter.CWD : File
    get() = File(this.get("bsh.cwd") as String)


    fun isWildcard(cand: String) = cand.isPattern

    fun ls(dir: Any?): Iterable<File> {
        when(dir) {
            is String -> {
                if(dir.isPattern) {
                    return expands(intp.CWD, dir).asIterable()
                } else {
                    return lsFile(intp.pathToFile(dir))
                }
            }
            is File -> {
                return lsFile(dir)
            }
        }
        return listOf<File>()
    }

    fun expands(dir: File, pattern: String) : Array<File> {
        val pat = buildString{
            pattern.forEach { c ->
                when(c) {
                    '.' -> append("\\.")
                    '\\' -> append("\\\\")
                    '*' -> append(".*")
                    else -> append(c)
                }
            }
        }.toRegex()
        return dir.listFiles{ d, name ->
            pat.matches(name)
        }
    }

    fun copyOne(src: File, dest: File) : Boolean {
        return try {
            src.copyTo(dest)
            true
        }catch(e: IOException) {
            false
        }
    }

    fun copyMany(srcs: Iterable<File>, destDir:File) : Boolean {
        if(!destDir.isDirectory)
            return false
        return srcs.all {
            val dest = File(destDir, it.name)
            copyOne(it, dest)
        }
    }

    fun copyCommand(files: Array<Any>) : Boolean {
        assert(files.size >= 2)

        try {
            val cwd = intp.CWD
            val srces = files.slice(0 until files.size-1).flatMap {
                when(it) {
                    is String -> {
                        if(it.isPattern) {
                            expands(cwd, it).asIterable()
                        } else {
                            arrayListOf(intp.pathToFile(it))
                        }
                    }
                    is File -> {
                        arrayListOf(it)
                    }
                    else -> throw IllegalArgumentException("Unknown arg")
                }
            }

            val last = files.last()
            val dest = when(last) {
                is String -> intp.pathToFile(last)
                is File -> last
                else -> throw IllegalArgumentException("unknown dest")
            }

            copyMany(srces, dest)

            return true

        }catch(e: IllegalArgumentException) {
            return false;
        }


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