package com.samrat.finalapp

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

object JsonDataLoader {

    private const val JSON_FILE_NAME = "diseases.json"

    fun loadDiseases(context: Context, cropName: String): List<Disease> {
        val inputStream = context.assets.open(JSON_FILE_NAME)
        val reader = InputStreamReader(inputStream)
        val type = object : TypeToken<Map<String, List<Disease>>>() {}.type
        val data: Map<String, List<Disease>> = Gson().fromJson(reader, type)
        reader.close()
        inputStream.close()
        return data[cropName] ?: emptyList()
    }
}
