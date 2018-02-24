package io.github.karino2.filescripting

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import bsh.EvalError
import bsh.ParseException
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import io.github.karino2.filescripting.ols.Interpreter
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    fun runCommand() {
        val script = etCmdLine.text.toString()
        etCmdLine.setText("")
        try {
            val res = olsInterpreter.parseToEnd(script)
            printObject(res)
            println("")
        }catch(e : Exception) {
            println("Exception: ${e.message}")
        }

    }

    val olsInterpreter by lazy {
        val intp = Interpreter(bshInterpreter, this)
        intp
    }


    val etCmdLine by lazy {
        findViewById(R.id.editTextCmdLine) as EditText
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // instantiation first for easier development
        val bintp = bshInterpreter

        val commandPublisher = PublishSubject.create<Int>()

        commandPublisher.throttleFirst(500L, TimeUnit.MILLISECONDS)
                .subscribe() {
                    when(it) {
                        EditorInfo.IME_ACTION_GO-> {
                            runCommand()
                        }
                        EditorInfo.IME_ACTION_UNSPECIFIED -> {
                            runCommand();
                        }
                    }
                }


        etCmdLine.setOnEditorActionListener(fun(view, actionId, keyEvent)  : Boolean {
            when(actionId) {
                EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_UNSPECIFIED /* for hardware keyboard. */ -> {
                    commandPublisher.onNext(actionId)
                    return true
                }

            }
            return false;
        })
    }

    fun perrorln(msg: String) { println(msg) }


    val bshInterpreter by lazy {
        val int = bsh.Interpreter()
        val builtin = Builtins(int, this)
        int.set("ctx", this)
        int.set("builtins", builtin)
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
            val result = bshInterpreter.eval(script)
            result?.let {
                printObject(result)
                println("")
            }
        }catch(e: ParseException) {
            println(e.message!!)
        }catch(e : EvalError) {
            println("Error: ${e.errorLineNumber}: ${e.errorText}")
        }
    }

    // do not print tail \n
    fun printObject(obj: Any?) {
        when(obj) {
            null ->{
                print("null")
            }
            is Iterable<*> -> {
                println(bsh.StringUtil.normalizeClassName(obj.javaClass) + ":")
                for(one in obj) {
                    printObject(one)
                    println("")
                }
            }
            is File -> {
                val dt = Date(obj.lastModified())
                //  2015/12/20     12:52
                val fmt = SimpleDateFormat("yyyy/MM/dd HH:mm")
                val dateStr =fmt.format(dt)

                val sizeStr = if(obj.isDirectory) { "          " } else { "%10d".format(obj.length()) }
                print("$dateStr  $sizeStr ${obj.name}")
            }
            else -> {
                print(obj.toString())
            }
        }
    }


    fun println(msg: String) {
        print(msg + "\n")
    }

    private fun print(msg: String) {
        val cns = (findViewById(R.id.textViewOutput) as TextView)
        val whole = cns.text.toString() + msg
        cns.text = whole

        val sv = findViewById(R.id.scrollView) as ScrollView
        sv.post { sv.fullScroll(ScrollView.FOCUS_DOWN); }
    }

    val initScript = """
bsh.help.ls = "usage: ls(dir)";
ls(dir) {
    return builtins.ls(dir);
}


bsh.help.print = "usage: print( value )";

void print( arg )
{
    ctx.printObject(arg);
    ctx.println("");
}

bsh.help.javap= "usage: javap( value )";

import bsh.ClassIdentifier;
import java.lang.reflect.Modifier;

javap( Object o )
{
	Class clas;
	if ( o instanceof ClassIdentifier )
		clas = this.caller.namespace.identifierToClass(o);
	else if ( o instanceof String )
	{
		if ( o.length() < 1 ) {
			error("javap: Empty class name.");
			return;
		}
		clas = this.caller.namespace.getClass((String)o);
	} else if ( o instanceof Class )
		clas = o;
	else
		clas = o.getClass();

	print( "Class "+clas+" extends " +clas.getSuperclass() );

	this.dmethods=clas.getDeclaredMethods();
	//print("------------- Methods ----------------");
	for(int i=0; i<dmethods.length; i++) {
		this.m = dmethods[i];
		if ( Modifier.isPublic( m.getModifiers() ) )
			print( m );
	}

	//print("------------- Fields ----------------");
	this.fields=clas.getDeclaredFields();
	for(int i=0; i<fields.length; i++) {
		this.f = fields[i];
		if ( Modifier.isPublic( f.getModifiers() ) )
			print( f );
	}
}

bsh.help.pathToFile = "usage: File pathToFile( String )";

File pathToFile( String filename ) {
	return this.interpreter.pathToFile( filename );
}


bsh.help.cd = "usage: cd( path )";

void cd( String pathname )
{
    this.file = pathToFile( pathname );

	if ( file.exists() && file.isDirectory() )
		bsh.cwd = file.getCanonicalPath();
	else
        print( "No such directory: "+pathname);
}

bsh.help.cp = "usage: cp( fromFile, toFile )";

cp( String fromFile, String toFile )
{
    this.from = pathToFile( fromFile );
    this.to = pathToFile( toFile );

	this.in = new BufferedInputStream( new FileInputStream( from ) );
	this.out = new BufferedOutputStream( new FileOutputStream( to ) );
	byte [] buff = new byte [ 32*1024 ];
	while ( (len = in.read( buff )) > 0 )
			out.write( buff, 0, len );
	in.close();
	out.close();
}

bsh.help.mv = "usage: mv( fromFile, toFile )";

mv( String fromFile, String toFile )
{
    this.from = pathToFile( fromFile );
    this.to = pathToFile( toFile );
	from.renameTo( to );
}


        """

}
