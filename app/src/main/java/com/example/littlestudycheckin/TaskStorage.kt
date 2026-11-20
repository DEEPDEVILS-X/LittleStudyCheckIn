package com.example.littlestudycheckin

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

private const val PREFERENCES_NAME = "study_tasks_preferences"
private const val TASKS_KEY = "tasks"

// 从 SharedPreferences 读取任务；第一次打开时使用默认任务。
fun loadTasks(context: Context, defaultTasks: List<StudyTask>): List<StudyTask> {
    val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    if (!preferences.contains(TASKS_KEY)) {
        return defaultTasks
    }

    val tasksJson = preferences.getString(TASKS_KEY, "[]") ?: "[]"

    return try {
        val jsonArray = JSONArray(tasksJson)
        val tasks = mutableListOf<StudyTask>()

        for (index in 0 until jsonArray.length()) {
            val taskJson = jsonArray.getJSONObject(index)
            tasks.add(
                StudyTask(
                    id = taskJson.getInt("id"),
                    title = taskJson.getString("title"),
                    isCompleted = taskJson.optBoolean("isCompleted", false)
                )
            )
        }

        tasks
    } catch (exception: Exception) {
        // 保存内容损坏时，使用空列表，避免应用无法打开。
        emptyList()
    }
}

// 把当前任务列表转换成 JSON，并保存到 SharedPreferences。
fun saveTasks(context: Context, tasks: List<StudyTask>) {
    val jsonArray = JSONArray()

    tasks.forEach { task ->
        jsonArray.put(
            JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("isCompleted", task.isCompleted)
            }
        )
    }

    context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(TASKS_KEY, jsonArray.toString())
        .apply()
}
