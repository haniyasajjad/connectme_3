package com.fast.assignment3

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class LocalPost(
    val postId: String,
    val userId: String,
    val caption: String,
    val media: String,
    val likes: Long,
    val timestamp: Long
)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "assignment3.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_POSTS = "posts"
        private const val COLUMN_POST_ID = "post_id"
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_CAPTION = "caption"
        private const val COLUMN_MEDIA = "media"
        private const val COLUMN_LIKES = "likes"
        private const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createPostsTable = """
            CREATE TABLE $TABLE_POSTS (
                $COLUMN_POST_ID TEXT PRIMARY KEY,
                $COLUMN_USER_ID TEXT,
                $COLUMN_CAPTION TEXT,
                $COLUMN_MEDIA TEXT,
                $COLUMN_LIKES INTEGER,
                $COLUMN_TIMESTAMP INTEGER
            )
        """.trimIndent()
        db.execSQL(createPostsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_POSTS")
        onCreate(db)
    }

    fun insertPost(post: LocalPost) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_POST_ID, post.postId)
            put(COLUMN_USER_ID, post.userId)
            put(COLUMN_CAPTION, post.caption)
            put(COLUMN_MEDIA, post.media)
            put(COLUMN_LIKES, post.likes)
            put(COLUMN_TIMESTAMP, post.timestamp)
        }
        db.insert(TABLE_POSTS, null, values)
        db.close()
    }

    fun getAllPosts(): List<LocalPost> {
        val posts = mutableListOf<LocalPost>()
        val db = readableDatabase
        val cursor = db.query(TABLE_POSTS, null, null, null, null, null, "$COLUMN_TIMESTAMP DESC")
        while (cursor.moveToNext()) {
            posts.add(
                LocalPost(
                    postId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POST_ID)),
                    userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                    caption = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAPTION)),
                    media = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEDIA)),
                    likes = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LIKES)),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                )
            )
        }
        cursor.close()
        db.close()
        return posts
    }
}