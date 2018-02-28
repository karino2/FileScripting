package io.github.karino2.filescripting

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import android.widget.TextView
import org.jetbrains.anko.db.SqlOrderDirection
import org.jetbrains.anko.db.select
import java.text.SimpleDateFormat
import java.util.*

class ScriptsListActivity : AppCompatActivity() {

    val adapter by lazy {
        SimpleCursorAdapter(this, R.layout.script_list_item, null, arrayOf("script", "lastModified"), intArrayOf(R.id.textViewScript, R.id.textViewDate))
    }

    val handler by lazy { Handler() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scripts_list)

        val lv = findViewById(R.id.listView) as ListView

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
        lv.choiceMode = ListView.CHOICE_MODE_SINGLE
        lv.setOnItemClickListener { adpt, view, pos, id ->
            val res = Intent()
            res.putExtra("SCRIPT_ID", id)
            setResult(RESULT_OK, res)
            finish()
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
