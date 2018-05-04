package io.github.karino2.filescripting.ols

import android.util.Log
import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.parser.Parser

import io.github.karino2.filescripting.MainActivity
import io.github.karino2.filescripting.SnapInterpreter
import org.snapscript.core.ExpressionEvaluator

/**
 * Created by _ on 2018/02/24.
 */
class OlsInterpreter(val bintp: SnapInterpreter, val ctx: MainActivity) :Grammar<Any?>() {
    val id by token("\\w+")
    val dot by token ("\\.")
    val dol by token ("\\$")
    val equal by token("=")
    val aster by token("\\*")
    val ws by token("\\s+")

    val lVar = dol * id map { (_, nametk) -> nametk.text }


    val argVar = dol * id map { (_, nametk) -> bintp.getVariable(nametk.text) }

    val argLiteralExp  = oneOrMore((id or aster or dot) use {text}) use { joinToString("") }
    val argExp = argLiteralExp or argVar
    val commandWithArg = (id * -ws * separatedTerms(argExp, ws, acceptZero = true))
            .map { (name, args) -> funCall(name.text, args) }
    val singularCommand = id .map { funCall(it.text, emptyList<Any>())}

    val command = commandWithArg or singularCommand

    val assign = (lVar *  -zeroOrMore(ws)*equal* -zeroOrMore(ws) * command).map {(lname, _, rval) ->  bintp.putVariable(lname,  rval) }
    val statements = command or assign or argVar

    override val rootParser: Parser<Any?> = statements

    inline val Any?.javaOrDefClass : java.lang.Class<out Any?>
    get() {
        return this?.javaClass ?: Class.forName("java.lang.Object")
    }

    fun funCall(name: String, args: List<Any?>) : Any? {

        val resolved = bintp.resolveFunction(name, args.toTypedArray())
        resolved?.let {
            return it.call().getValue();
        }

        throw Exception("Undefined function: ${name}")
    }

}