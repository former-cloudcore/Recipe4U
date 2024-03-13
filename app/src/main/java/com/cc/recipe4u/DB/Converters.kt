package com.cc.recipe4u.DB

import androidx.room.TypeConverter
import com.cc.recipe4u.DataClass.Comment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromString(value: String): List<String> {
        val listType = object : com.google.common.reflect.TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromCommentList(comments: List<Comment>?): String? {
        return Gson().toJson(comments)
    }

    @TypeConverter
    fun toCommentList(commentsString: String?): List<Comment>? {
        return Gson().fromJson(commentsString, object : TypeToken<List<Comment>>() {}.type)
    }
}
