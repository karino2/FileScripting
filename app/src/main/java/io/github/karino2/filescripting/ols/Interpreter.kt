package io.github.karino2.filescripting.ols

import android.util.Log
import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.parser.Parser

import io.github.karino2.filescripting.MainActivity

/**
 * Created by _ on 2018/02/24.
 */
class Interpreter(val bintp: bsh.Interpreter, val ctx: MainActivity) :Grammar<Any?>() {
    val id by token("\\w+")
    val dot by token ("\\.")
    val dotdot by token ("\\.\\.")

    val argExp = id or dot or dotdot use { text }
    val ws by token("\\s+", ignore = true)
    val command by (id * separatedTerms(argExp, ws, acceptZero = true))
            .map { (name, args) -> funCall(name.text, args) }

    override val rootParser: Parser<Any?> by command


    fun funCall(name: String, args: List<String>) : Any? {
        val bshMethod = bintp.nameSpace.getMethod(name, args.map { it.javaClass  } .toTypedArray(), false)
        // val bshMethod = bintp.nameSpace.getMethod(name, args.map { it.javaClass as java.lang.Class<out Any?> } .toTypedArray(), false)
        // val bshMethod = bshMethodOrg ?: bintp.nameSpace.getMethod(name, args.map { Class.forName("java.lang.Object") as java.lang.Class<out Any?> } .toTypedArray(), false)
        if(bshMethod == null) {
            // val bshMethod3 = bintp.nameSpace.getMethod(name, arrayOf<java.lang.Class<out Any?>>(Class.forName("java.lang.Object")), false)


            val objarrClass = Class.forName("[Ljava.lang.Object;")
            val argTypes = arrayOf(objarrClass)
            val bshArrArgMethod = bintp.nameSpace.getMethod(name, args.map { it.javaClass } .toTypedArray(), false)
            if(bshArrArgMethod == null)
                throw Exception("Undefined function: ${name}")
            return bshArrArgMethod.invoke(arrayOf<java.lang.Object>(args as java.lang.Object), bintp)
        }

        val pt = bshMethod.parameterTypes
        Log.d("SF", "size = ${pt.size}")
        if(bshMethod.parameterTypes.size != args.size) {
            throw Exception("Unmatched argnum of function: ${name}. defined ${bshMethod.parameterTypes.size}, supplied ${args.size}")
        }
        return bshMethod.invoke(args.toTypedArray(), bintp)
    }

}