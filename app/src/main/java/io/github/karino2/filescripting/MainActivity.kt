package io.github.karino2.filescripting

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import bsh.EvalError
import bsh.ParseException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }


    val initScript = """
        print(String msg) {
            ctx.consoleWriteN(msg);
        }
        """

    val interpreter by lazy {
        val int = bsh.Interpreter()
        int.set("ctx", this)
        int.eval(initScript)
        int
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
         menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_run -> {
                runScript()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun runScript() {
        val script = (findViewById(R.id.editTextScript) as EditText).text.toString()
        try {
            val result = interpreter.eval(script)
            val resStr = result?.toString() ?: ""
            consoleWriteN(resStr)

        }catch(e: ParseException) {
            consoleWriteN(e.message!!)
        }catch(e : EvalError) {
            consoleWriteN("Error: ${e.errorLineNumber}: ${e.errorText}")
        }
    }

    fun consoleWriteN(msg: String) {
        val cns = (findViewById(R.id.textViewOutput) as TextView)
        val whole = msg + "\n" + cns.text.toString()
        cns.text = whole
    }


}
