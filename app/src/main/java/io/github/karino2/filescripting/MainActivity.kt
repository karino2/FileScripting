package io.github.karino2.filescripting

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    val interpreter by lazy {
        bsh.Interpreter()
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
        val result = interpreter.eval(script)
        consoleWriteN(result.toString())
    }

    private fun consoleWriteN(msg: String) {
        val cns = (findViewById(R.id.textViewOutput) as TextView)
        val whole = msg + "\n" + cns.text.toString()
        cns.text = whole
    }


}
