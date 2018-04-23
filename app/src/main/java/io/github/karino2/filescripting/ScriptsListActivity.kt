package io.github.karino2.filescripting

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.AbsListView
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import android.widget.TextView
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.db.SqlOrderDirection
import org.jetbrains.anko.db.select
import java.text.SimpleDateFormat
import java.util.*

class ScriptsListActivity : AppCompatActivity() {

    val adapter by lazy {
        SimpleCursorAdapter(this, R.layout.script_list_item, null, arrayOf("script", "lastModified"), intArrayOf(R.id.textViewScript, R.id.textViewDate))
    }

    val handler by lazy { Handler() }

    val listView by lazy {
        findViewById<ListView>(R.id.listView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scripts_list)

        val lv = listView

        handler.post {
                val cursor = database.query("Scripts", arrayOf("_id", "script", "lastModified"),
                        null, null, null, null, "lastModified DESC, _id DESC")
                adapter.swapCursor(cursor)
                startManagingCursor(cursor)
        }

        adapter.setViewBinder { view, cursor, columnIndex ->
            when(columnIndex) {
                1-> {
                    val tv = view as TextView
                    val script = cursor.getString(columnIndex)
                    tv.setText(script.makeEllipsisIfTooLong)
                    tv.setTag(cursor.getLong(0)); // ID
                    true
                }
                 2 -> {
                     val tv = view as TextView
                     val sdf = SimpleDateFormat("yyyy/MM/dd")
                     tv.setText(sdf.format(Date(cursor.getLong(columnIndex))))
                     true
                }
                else ->false
            }
        }

        lv.adapter = adapter
        lv.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
        lv.setOnItemClickListener { adpt, view, pos, id ->
            val res = Intent()
            res.putExtra("SCRIPT_ID", id)
            setResult(RESULT_OK, res)
            finish()
        }

        lv.setMultiChoiceModeListener(createMultiChoiceModeListener());
    }

    fun createMultiChoiceModeListener() : AbsListView.MultiChoiceModeListener {
        return object : AbsListView.MultiChoiceModeListener{
            override fun onItemCheckedStateChanged(p0: ActionMode?, p1: Int, p2: Long, p3: Boolean) {
            }

            override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
                actionMode.menuInflater.inflate(R.menu.scripts_context, menu)
                return true
            }

            override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?): Boolean {
                return false;
            }

            override fun onDestroyActionMode(p0: ActionMode?) {
            }

            override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
                return when(menuItem.itemId) {
                    R.id.action_delete -> {
                        val scriptIds = listView.checkedItemIds
                        deleteScripts(scriptIds)
                        actionMode.finish()
                        true
                    }
                    else -> {
                        false
                    }
                }
            }

        }
    }

    val Context.database: SQLiteDatabase
        get() = MyDatabaseOpenHelper.getInstance(getApplicationContext()).writableDatabase

    private fun deleteScripts(scriptIds: LongArray) {
        Completable.fromAction {
            database.deleteScripts(scriptIds)
        }.subscribeOn(Schedulers.io())
        .subscribe {
            supportInvalidateOptionsMenu()
        }
    }

    val TOOLONG_LINE_NUM = 12
    val String.makeEllipsisIfTooLong : String
    get() {
        val lineList = this.split("\n")
        if(lineList.size <= TOOLONG_LINE_NUM)
            return this;
        val builder = StringBuilder()
        for(i in 0 until TOOLONG_LINE_NUM) {
            builder.append(lineList.get(i))
        }
        builder.append("...");
        return builder.toString();
    }



}
