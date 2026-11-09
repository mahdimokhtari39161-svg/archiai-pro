package com.archiaipro.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.archiaipro.data.DesignDao
import com.archiaipro.data.DesignEntity
import com.archiaipro.data.ProjectDao
import com.archiaipro.data.ProjectEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

suspend fun shareBackup(context: Context, projects: ProjectDao, designs: DesignDao) {
    val projectList = projects.getAll()
    val designList = designs.getAll()
    val root = JSONObject().put("format", "archiai-backup-v1").put("exportedAt", System.currentTimeMillis())
    val projectArray = JSONArray()
    projectList.forEach { p ->
        projectArray.put(JSONObject()
            .put("id", p.id).put("name", p.name).put("type", p.type)
            .put("location", p.location).put("area", p.area)
            .put("description", p.description).put("createdAt", p.createdAt).put("updatedAt", p.updatedAt))
    }
    val designArray = JSONArray()
    designList.forEach { d ->
        designArray.put(JSONObject()
            .put("id", d.id).put("projectId", d.projectId).put("kind", d.kind)
            .put("title", d.title).put("payloadJson", d.payloadJson).put("updatedAt", d.updatedAt))
    }
    root.put("projects", projectArray).put("designs", designArray)
    val folder = File(context.cacheDir, "backups").apply { mkdirs() }
    val output = File(folder, "archiai-backup.json")
    output.writeText(root.toString(2), Charsets.UTF_8)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", output)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share ArchiAI backup"))
}

suspend fun restoreBackup(context: Context, uri: Uri, projectsDao: ProjectDao, designsDao: DesignDao): Int {
    val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        ?: error("فایل پشتیبان قابل خواندن نیست")
    val root = JSONObject(text)
    require(root.optString("format") == "archiai-backup-v1") { "فرمت فایل پشتیبان معتبر نیست" }
    val projectArray = root.optJSONArray("projects") ?: JSONArray()
    val restoredProjects = (0 until projectArray.length()).map { index ->
        val p = projectArray.getJSONObject(index)
        ProjectEntity(
            id = p.optLong("id"),
            name = p.optString("name"),
            type = p.optString("type"),
            location = p.optString("location"),
            area = p.optDouble("area"),
            description = p.optString("description"),
            createdAt = p.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = p.optLong("updatedAt", System.currentTimeMillis())
        )
    }
    projectsDao.restoreAll(restoredProjects)
    val designArray = root.optJSONArray("designs") ?: JSONArray()
    val restoredDesigns = (0 until designArray.length()).map { index ->
        val d = designArray.getJSONObject(index)
        DesignEntity(
            id = d.optLong("id"),
            projectId = d.optLong("projectId"),
            kind = d.optString("kind"),
            title = d.optString("title"),
            payloadJson = d.optString("payloadJson"),
            updatedAt = d.optLong("updatedAt", System.currentTimeMillis())
        )
    }.filter { item -> restoredProjects.any { it.id == item.projectId } }
    designsDao.restoreAll(restoredDesigns)
    return restoredProjects.size
}
