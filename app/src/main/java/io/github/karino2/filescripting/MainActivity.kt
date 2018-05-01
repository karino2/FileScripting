package io.github.karino2.filescripting

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import io.github.karino2.filescripting.ols.Interpreter
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.snapscript.common.store.ClassPathStore
import org.snapscript.compile.StoreContext
import org.snapscript.core.scope.MapModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {


    val LIST_SCRIPT_REQUEST = 1
    fun runCommand() {
        val script = etCmdLine.text.toString()
        etCmdLine.setText("")
        try {
            val res = olsInterpreter.parseToEnd(script)
            /*
            if(res == bsh.Primitive.VOID)
                return
            */
            res?.let {
                printObject(res)
                println("")
            }
        }catch(e : Exception) {
            println("Exception: ${e.message}")
        }

    }

    val olsInterpreter by lazy {
        val intp = Interpreter(evaluator, this)
        intp
    }


    val etCmdLine by lazy {
        findViewById(R.id.editTextCmdLine) as EditText
    }

    val tabLayout by lazy {
        findViewById(R.id.tabLayout) as TabLayout
    }
    val TabLayout.currentTab
    get() = this.getTabAt(this.selectedTabPosition)

    override fun onStop() {
        tabLayout.currentTab?.let {
            saveIfNecessary(it)
        }
         super.onStop()
    }

    val model by lazy {
        MapModel(emptyMap<String, Any>())
    }

    val evaluator by lazy {
        val store = ClassPathStore()
        val context = StoreContext(store)
        context.evaluator
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etScript.customSelectionActionModeCallback =
                object : android.view.ActionMode.Callback {
                    override fun onActionItemClicked(actionMode: android.view.ActionMode, menuItem: MenuItem): Boolean {
                        when(menuItem.itemId){
                            R.id.action_run_selected -> {

                                evalSelected()
                                actionMode.finish()
                                return true
                            }

                        }
                        return false
                    }

                    override fun onCreateActionMode(actionMode: android.view.ActionMode, menu: Menu): Boolean {
                        menu.add(Menu.NONE, R.id.action_run_selected, Menu.CATEGORY_SYSTEM, "Run").setIcon(android.R.drawable.ic_media_play)
                                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                        return true
                    }

                    override fun onPrepareActionMode(p0: android.view.ActionMode?, p1: Menu?) = true

                    override fun onDestroyActionMode(p0: android.view.ActionMode?) {
                    }
                }

        etScript.setOnKeyListener { view, keyCode, keyEvent ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN &&
                    keyEvent.isShiftPressed) {
                evalSelected()
                true
            }else {
                false
            }
        }

        val script = ScriptModel()

        // for scratch, old and cur must is the same.
        // tabLayout.getTabAt(0)!!.tag = Pair(script, script.copy())
        tabLayout.getTabAt(0)!!.tag = Pair(script, script)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
            override fun onTabUnselected(tab: TabLayout.Tab) {
                saveIfNecessary(tab)
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                val (_, cur) = (tab.tag as Pair<ScriptModel, ScriptModel>)
                etScript.setText(cur.script)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

        })



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

    private fun evalSelected(){
        val start = etScript.selectionStart
        val end = etScript.selectionEnd

        if (start == -1 || end == -1)
            return

        val script = etScript.text.substring(
                Math.min(start, end),
                Math.max(start, end)
        )


        runScript(script)
    }

    val TabLayout.Tab.isScratch
    get() = this.text == "*scratch*"

    private fun saveIfNecessary(tab: TabLayout.Tab) {

        // when close, tab.tag is null.
        tab.tag?.let {
            val (old, cur) = (it as Pair<ScriptModel, ScriptModel>)
            cur.script = etScript.text.toString()

            if(tab.isScratch) {
                return
            }

            if (old != cur) {
                cur.lastModified = Date().time

                database.updateScript(cur)
                tab.tag = Pair(cur.copy(), cur)
            }
        }
    }

    fun perrorln(msg: String) { println(msg) }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
         menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_run -> {
                runEditTextScript()
                return true
            }
            R.id.action_new -> {
                //         tabLayout.getTabAt(0)!!.tag = Pair(script, script.copy())
                val script = ScriptModel()
                openTabWith(script)
                return true
            }
            R.id.action_list -> {
                val intent = Intent(this, ScriptsListActivity::class.java)
                startActivityForResult(intent, LIST_SCRIPT_REQUEST)
                return true
            }
            R.id.action_close -> {
                tabLayout.currentTab?.let {
                    if(it.isScratch) {
                        println("Can't close scratch.")
                        return true
                    }

                    saveIfNecessary(it)

                    it.tag = null;
                    tabLayout.removeTab(it)
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openTabWith(script: ScriptModel) {
        val tab = tabLayout.newTab()
        tab.text = "*New*"
        tab.tag = Pair(script, script.copy())
        tabLayout.addTab(tab)
        tab.select()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            LIST_SCRIPT_REQUEST -> {
                if(resultCode == RESULT_OK) {
                    openScript(data!!.getLongExtra("SCRIPT_ID", -1))
                }
                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun openScript(scriptId: Long) {
        val script = database.selectScript(scriptId)
        openTabWith(script)

    }

    val etScript by lazy {
        findViewById<EditText>(R.id.editTextScript)
    }

    private fun runEditTextScript() {
        val script = etScript.text.toString()
        runScript(script)
    }

    var running = false;
    var startTick = 0L


    private fun runScript(script: String) {
        if(running) {
            showMessage("already running...")
            return
        }

        running = true
        startTick = System.currentTimeMillis()
        showMessage("run")
        Flowable.fromCallable {
            evaluator.evaluate<Any>(model, script)
        }.subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {
                            if(System.currentTimeMillis() - startTick> 5000) {
                                showMessage("eval done.")
                            }

                            running = false
                            it?.let {
                                printObject(it)
                                println("")
                            }
                        },
                        {
                            running = false
                            when(it)
                            {
                                is Exception ->
                                {
                                    println(it.message!!)
                                }
                                /*
                                is ParseException ->
                                {
                                    println(it.message!!)
                                }
                                is EvalError -> {
                                    println("Error: ${it.errorLineNumber}: ${it.errorText}\n ${it.message}")
                                }
                                */
                            }

                        }
                )
    }



    private fun showMessage(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    // do not print tail \n
    fun printObject(obj: Any?) {
        when(obj) {
            null ->{
                print("null")
            }
            is Iterable<*> -> {
                // println(bsh.StringUtil.normalizeClassName(obj.javaClass) + ":")
                println(obj.javaClass.toString() + ":")
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
        Completable.fromAction {
            val cns = (findViewById(R.id.textViewOutput) as TextView)
            val whole = cns.text.toString() + msg
            cns.text = whole

            val sv = findViewById(R.id.scrollView) as ScrollView
            sv.post { sv.fullScroll(ScrollView.FOCUS_DOWN); }

        }.subscribeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    val initScript = """
bsh.help.ls = "usage: ls(dir)";
ls(dir) {
    return builtins.ls(dir);
}
ls() {
    return builtins.ls(".");
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

bsh.help.mkdir = "usage: mkdir(path)";

mkdir(pathname)
{
    this.file = pathToFile( pathname );

	if ( this.file.exists() ) {
        print( "Already exist: "+pathname);
        return this.file;
    }
    if(!this.file.mkdir()) {
        print("Fail to create: " + pathname);
        return this.file;
    }
    return this.file;

}


bsh.help.pwd = "usage: pwd()";

pwd()
{
    return bsh.cwd;
}

bsh.help.cd = "usage: cd( path )";

void cd( String pathname )
{
    if("..".equals(pathname)) {
      this.cur = new File(bsh.cwd);
      this.file = this.cur.getParentFile();
      if(this.file == null) {
        return;
      }
    } else if(builtins.isWildcard(pathname)) {
        this.files = builtins.expands(new File(bsh.cwd), pathname);
        if(this.files.length == 0) {
            print( "No such directory: "+pathname);
        }
        this.file = this.files[0];
    } else {
        this.file = pathToFile( pathname );
    }

	if ( file.exists() && file.isDirectory() )
		bsh.cwd = file.getCanonicalPath();
	else
        print( "No such directory: "+pathname);
}

bsh.help.cp = "usage: cp( fileArray )\nlast element is destination.";

void cp( Object[] files )
{
    if(files.length < 2) {
        print("cp src [src2, src3, ...] dest");
        return;
    }
    builtins.copyCommand(files);
}

bsh.help.mv = "usage: mv( fileArray )\nlast element is destination.";

void mv( Object[] files )
{
    if(files.length < 2) {
        print("mv src [src2, src3, ...] dest");
        return;
    }
    builtins.moveCommand(files);
}

bsh.help.rm = "usage: rm( fileArray )";

void rm( Object[] files )
{
    builtins.rmCommand(files);
}

        """

}
