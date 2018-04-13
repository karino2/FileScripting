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
    val dol by token ("\\$")
    val equal by token("=")
    val aster by token("\\*")

    val lVar = dol * id map { (_, nametk) -> nametk.text }


    val argVar = dol * id map { (_, nametk) -> bintp.get(nametk.text) }

    val argLiteralExp  = oneOrMore((id or aster or dot) use {text}) use { joinToString("") }
    val argExp = argLiteralExp or argVar
    val ws by token("\\s+", ignore = true)
    val command = (id * separatedTerms(argExp, ws, acceptZero = true))
            .map { (name, args) -> funCall(name.text, args) }


    val assign = (lVar * equal * command).map {(lname, _, rval) ->   bintp.set(lname,  rval) }
    val statements = command or assign or argVar

    override val rootParser: Parser<Any?> = statements

    inline val Any?.javaOrDefClass : java.lang.Class<out Any?>
    get() {
        return this?.javaClass ?: Class.forName("java.lang.Object")
    }

    fun funCall(name: String, args: List<Any?>) : Any? {
        // it?.javaClass ?: Class.forName("java.lang.Object")

        val bshMethod = bintp.nameSpace.getMethod(name, args.map { it.javaOrDefClass } .toTypedArray(), false)
        // val bshMethod = bintp.nameSpace.getMethod(name, args.map { it.javaClass as java.lang.Class<out Any?> } .toTypedArray(), false)
        // val bshMethod = bshMethodOrg ?: bintp.nameSpace.getMethod(name, args.map { Class.forName("java.lang.Object") as java.lang.Class<out Any?> } .toTypedArray(), false)
        if(bshMethod == null) {
            // val bshMethod3 = bintp.nameSpace.getMethod(name, arrayOf<java.lang.Class<out Any?>>(Class.forName("java.lang.Object")), false)


            val objarrClass = Class.forName("[Ljava.lang.Object;")
            val argTypes = arrayOf(objarrClass)
            val bshArrArgMethod = bintp.nameSpace.getMethod(name, args.map { it.javaOrDefClass } .toTypedArray(), false)
            if(bshArrArgMethod == null)
                throw Exception("Undefined function: ${name}")
            return bshArrArgMethod.invoke(arrayOf<java.lang.Object>(args as java.lang.Object), bintp)
        }

        if(bshMethod.parameterTypes.size != args.size) {
            throw Exception("Unmatched argnum of function: ${name}. defined ${bshMethod.parameterTypes.size}, supplied ${args.size}")
        }
        return bshMethod.invoke(args.toTypedArray(), bintp)
    }

}