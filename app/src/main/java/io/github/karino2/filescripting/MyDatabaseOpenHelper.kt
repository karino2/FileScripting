package io.github.karino2.filescripting

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.jetbrains.anko.db.*

/**
 * Created by _ on 2018/02/27.
 */
class MyDatabaseOpenHelper(ctx: Context) : SQLiteOpenHelper(ctx, "MyDatabase", null, 1) {
    companion object {
        private var instance: MyDatabaseOpenHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): MyDatabaseOpenHelper {
            if (instance == null) {
                instance = MyDatabaseOpenHelper(ctx.getApplicationContext())
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.createTable("Scripts", true,
                "_id" to INTEGER + PRIMARY_KEY + UNIQUE,
                "script" to TEXT,
                "lastModified" to INTEGER)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // db.dropTable("User", true)
    }

}

// Access property for Context
val Context.database: SQLiteDatabase
    get() = MyDatabaseOpenHelper.getInstance(getApplicationContext()).writableDatabase

fun SQLiteDatabase.updateScript(model : ScriptModel) {
    if(model.id == -1L) {
        val id = this.insert("Scripts",
                "script" to model.script,
                "lastModified" to model.lastModified
        )
        model.id = id
    }else {
        this.update("Scripts",
                "script" to model.script,
                "lastModified" to model.lastModified
                )
                .whereArgs("_id={sid}", "sid" to model.id)
                .exec()

    }
}

fun SQLiteDatabase.deleteScripts(ids : LongArray) {
    this.delete("Scripts", "_id in (${ids.joinToString()})")
}


fun SQLiteDatabase.selectScript(id: Long) : ScriptModel {
    return this.select("Scripts", "_id", "script", "lastModified")
            .whereArgs("_id={sid}", "sid" to id)
            .exec {
                moveToFirst()
        ScriptModel(getLong(0), getString(1), getLong(2))
    }
}