package com.example.littlestudycheckin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.littlestudycheckin.ui.theme.LittleStudyCheckInTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LittleStudyCheckInTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    StudyCheckInScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// 学习任务：新建时默认未完成。
data class StudyTask(
    val id: Int,
    val title: String,
    val isCompleted: Boolean = false
)

// 第一次打开应用时显示的示例任务。
private val defaultStudyTasks = listOf(
    StudyTask(id = 1, title = "阅读 Kotlin 基础教程"),
    StudyTask(id = 2, title = "完成一道 C++ 练习题"),
    StudyTask(id = 3, title = "整理今天的学习笔记")
)

// 主页面：组合标题、输入区域、任务列表和完成进度。
@Composable
fun StudyCheckInScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // 保存输入框文字。
    var taskInput by remember { mutableStateOf("") }
    // 第一次打开使用默认任务，之后从 SharedPreferences 读取上次保存的任务。
    val tasks = remember {
        mutableStateListOf<StudyTask>().apply {
            addAll(loadTasks(context, defaultStudyTasks))
        }
    }
    // 根据已读取的最大 ID 继续编号，避免新任务和旧任务重复。
    var nextTaskId by remember {
        mutableStateOf((tasks.maxOfOrNull { it.id } ?: 0) + 1)
    }
    // 从当前列表统计，不另外保存一份完成数量。
    val completedCount = tasks.count { it.isCompleted }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "小小学习打卡",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(text = "每天完成一点学习任务，积累自己的进步。")

        TaskInputSection(
            taskInput = taskInput,
            onTaskInputChange = { taskInput = it },
            onAddTask = {
                // 去掉首尾空白，避免添加空任务。
                val taskTitle = taskInput.trim()
                if (taskTitle.isNotEmpty()) {
                    tasks.add(StudyTask(id = nextTaskId, title = taskTitle))
                    nextTaskId += 1
                    taskInput = ""
                    saveTasks(context, tasks)
                }
            }
        )

        Text(
            text = "今日任务",
            style = MaterialTheme.typography.titleMedium
        )

        TaskList(
            tasks = tasks,
            modifier = Modifier.weight(1f),
            onCompletedChange = { task, isCompleted ->
                // 用 ID 找到当前任务，再用 copy 创建更新后的任务。
                val index = tasks.indexOfFirst { it.id == task.id }
                if (index != -1) {
                    tasks[index] = tasks[index].copy(isCompleted = isCompleted)
                    saveTasks(context, tasks)
                }
            },
            onDelete = { task ->
                tasks.removeAll { it.id == task.id }
                saveTasks(context, tasks)
            }
        )

        TaskProgressText(completedCount = completedCount, totalCount = tasks.size)
    }
}

// 输入框区域：只负责接收文字和触发添加回调。
@Composable
fun TaskInputSection(
    taskInput: String,
    onTaskInputChange: (String) -> Unit,
    onAddTask: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextField(
            value = taskInput,
            onValueChange = onTaskInputChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text("学习任务") },
            placeholder = { Text("输入一个学习任务") }
        )
        Button(onClick = onAddTask) {
            Text("添加")
        }
    }
}

// 任务列表区域：负责显示任务或空列表提示。
@Composable
fun TaskList(
    tasks: List<StudyTask>,
    modifier: Modifier = Modifier,
    onCompletedChange: (StudyTask, Boolean) -> Unit,
    onDelete: (StudyTask) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (tasks.isEmpty()) {
            item {
                Text(
                    text = "还没有学习任务，添加一个吧",
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        } else {
            // ID 是每个列表项的稳定标识，同名任务也有不同 ID。
            items(tasks, key = { it.id }) { task ->
                StudyTaskCard(
                    task = task,
                    onCompletedChange = { isCompleted ->
                        onCompletedChange(task, isCompleted)
                    },
                    onDelete = { onDelete(task) }
                )
            }
        }
    }
}

// 进度文字：只负责显示已完成数量和总数量。
@Composable
fun TaskProgressText(completedCount: Int, totalCount: Int) {
    Text(
        text = "完成进度：$completedCount / $totalCount",
        style = MaterialTheme.typography.titleMedium
    )
}

// 任务卡片：显示完成状态，并把切换和删除操作交给主页面处理。
@Composable
fun StudyTaskCard(
    task: StudyTask,
    onCompletedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onCompletedChange
            )
            Text(
                text = task.title,
                modifier = Modifier.weight(1f),
                textDecoration = if (task.isCompleted) {
                    TextDecoration.LineThrough
                } else {
                    TextDecoration.None
                },
                color = if (task.isCompleted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            TextButton(onClick = onDelete) {
                Text("删除")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StudyCheckInPreview() {
    LittleStudyCheckInTheme {
        StudyCheckInScreen()
    }
}
