package io.github.karino2.filescripting

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import bsh.EvalError
import bsh.ParseException
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }



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
