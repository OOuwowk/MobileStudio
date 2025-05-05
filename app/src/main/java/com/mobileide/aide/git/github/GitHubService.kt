package com.mobileide.aide.git.github

import android.content.Context
import android.util.Log
import com.mobileide.aide.git.GitResult
import com.mobileide.aide.git.GitService
import com.mobileide.compiler.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * خدمة GitHub المسؤولة عن التفاعل مع GitHub API
 */
@Singleton
class GitHubService @Inject constructor(
    private val context: Context,
    private val gitService: GitService
) {
    companion object {
        private const val TAG = "GitHubService"
        private const val API_URL = "https://api.github.com"
    }
    
    private val client = OkHttpClient()
    
    /**
     * تسجيل الدخول إلى GitHub
     */
    suspend fun login(token: String): GitHubResult = withContext(Dispatchers.IO) {
        try {
            // التحقق من صحة الرمز
            val request = Request.Builder()
                .url("$API_URL/user")
                .header("Authorization", "token $token")
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "{}"
                    val json = JSONObject(body)
                    
                    val user = GitHubUser(
                        id = json.optInt("id"),
                        login = json.optString("login"),
                        name = json.optString("name"),
                        avatarUrl = json.optString("avatar_url"),
                        email = json.optString("email")
                    )
                    
                    return@withContext GitHubResult(
                        success = true,
                        message = "تم تسجيل الدخول بنجاح",
                        data = user
                    )
                } else {
                    return@withContext GitHubResult(
                        success = false,
                        message = "فشل تسجيل الدخول: ${response.message}",
                        data = null
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging in to GitHub", e)
            return@withContext GitHubResult(
                success = false,
                message = "فشل تسجيل الدخول: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * الحصول على قائمة المستودعات
     */
    suspend fun getRepositories(token: String): GitHubResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$API_URL/user/repos?sort=updated&per_page=100")
                .header("Authorization", "token $token")
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(body)
                    
                    val repositories = mutableListOf<GitHubRepository>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        
                        val repository = GitHubRepository(
                            id = json.optInt("id"),
                            name = json.optString("name"),
                            fullName = json.optString("full_name"),
                            description = json.optString("description"),
                            url = json.optString("html_url"),
                            cloneUrl = json.optString("clone_url"),
                            private = json.optBoolean("private"),
                            fork = json.optBoolean("fork"),
                            owner = json.optJSONObject("owner")?.optString("login") ?: ""
                        )
                        
                        repositories.add(repository)
                    }
                    
                    return@withContext GitHubResult(
                        success = true,
                        message = "تم الحصول على قائمة المستودعات بنجاح",
                        data = repositories
                    )
                } else {
                    return@withContext GitHubResult(
                        success = false,
                        message = "فشل الحصول على قائمة المستودعات: ${response.message}",
                        data = null
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting GitHub repositories", e)
            return@withContext GitHubResult(
                success = false,
                message = "فشل الحصول على قائمة المستودعات: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * إنشاء مستودع جديد
     */
    suspend fun createRepository(token: String, name: String, description: String, isPrivate: Boolean): GitHubResult = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("name", name)
                put("description", description)
                put("private", isPrivate)
                put("auto_init", true)
            }
            
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$API_URL/user/repos")
                .header("Authorization", "token $token")
                .post(requestBody)
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "{}"
                    val jsonResponse = JSONObject(body)
                    
                    val repository = GitHubRepository(
                        id = jsonResponse.optInt("id"),
                        name = jsonResponse.optString("name"),
                        fullName = jsonResponse.optString("full_name"),
                        description = jsonResponse.optString("description"),
                        url = jsonResponse.optString("html_url"),
                        cloneUrl = jsonResponse.optString("clone_url"),
                        private = jsonResponse.optBoolean("private"),
                        fork = jsonResponse.optBoolean("fork"),
                        owner = jsonResponse.optJSONObject("owner")?.optString("login") ?: ""
                    )
                    
                    return@withContext GitHubResult(
                        success = true,
                        message = "تم إنشاء المستودع بنجاح",
                        data = repository
                    )
                } else {
                    return@withContext GitHubResult(
                        success = false,
                        message = "فشل إنشاء المستودع: ${response.message}",
                        data = null
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating GitHub repository", e)
            return@withContext GitHubResult(
                success = false,
                message = "فشل إنشاء المستودع: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * استنساخ مستودع
     */
    suspend fun cloneRepository(token: String, repository: GitHubRepository, directory: File): GitHubResult = withContext(Dispatchers.IO) {
        try {
            // إنشاء URL مع الرمز
            val cloneUrl = repository.cloneUrl.replace("https://", "https://$token@")
            
            // استنساخ المستودع
            val result = gitService.cloneRepository(cloneUrl, directory)
            
            return@withContext if (result.success) {
                GitHubResult(
                    success = true,
                    message = "تم استنساخ المستودع بنجاح",
                    data = repository
                )
            } else {
                GitHubResult(
                    success = false,
                    message = "فشل استنساخ المستودع: ${result.message}",
                    data = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cloning GitHub repository", e)
            return@withContext GitHubResult(
                success = false,
                message = "فشل استنساخ المستودع: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * دفع المشروع إلى مستودع GitHub
     */
    suspend fun pushProject(token: String, project: Project, repository: GitHubRepository): GitHubResult = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(project.path)
            
            // التحقق من وجود مستودع Git
            val gitDir = File(projectDir, ".git")
            if (!gitDir.exists()) {
                // تهيئة مستودع Git
                val initResult = gitService.initRepository(project)
                if (!initResult.success) {
                    return@withContext GitHubResult(
                        success = false,
                        message = "فشل تهيئة مستودع Git: ${initResult.message}",
                        data = null
                    )
                }
            }
            
            // تعيين إعدادات Git
            gitService.setConfig(project, "user.name", "AIDE User")
            gitService.setConfig(project, "user.email", "aide@example.com")
            
            // إضافة remote
            val remoteUrl = repository.cloneUrl.replace("https://", "https://$token@")
            val addRemoteResult = gitService.addRemote(project, "origin", remoteUrl)
            
            // إضافة جميع الملفات
            val addResult = gitService.addAllFiles(project)
            if (!addResult.success) {
                return@withContext GitHubResult(
                    success = false,
                    message = "فشل إضافة الملفات: ${addResult.message}",
                    data = null
                )
            }
            
            // عمل commit
            val commitResult = gitService.commit(project, "Initial commit from AIDE")
            if (!commitResult.success) {
                return@withContext GitHubResult(
                    success = false,
                    message = "فشل عمل commit: ${commitResult.message}",
                    data = null
                )
            }
            
            // دفع التغييرات
            val pushResult = gitService.push(project)
            
            return@withContext if (pushResult.success) {
                GitHubResult(
                    success = true,
                    message = "تم دفع المشروع بنجاح",
                    data = repository
                )
            } else {
                GitHubResult(
                    success = false,
                    message = "فشل دفع المشروع: ${pushResult.message}",
                    data = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing project to GitHub", e)
            return@withContext GitHubResult(
                success = false,
                message = "فشل دفع المشروع: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * إنشاء طلب سحب
     */
    suspend fun createPullRequest(
        token: String,
        repository: GitHubRepository,
        title: String,
        body: String,
        head: String,
        base: String
    ): GitHubResult = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("title", title)
                put("body", body)
                put("head", head)
                put("base", base)
            }
            
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$API_URL/repos/${repository.fullName}/pulls")
                .header("Authorization", "token $token")
                .post(requestBody)
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "{}"
                    val jsonResponse = JSONObject(responseBody)
                    
                    val pullRequest = GitHubPullRequest(
                        id = jsonResponse.optInt("id"),
                        number = jsonResponse.optInt("number"),
                        title = jsonResponse.optString("title"),
                        body = jsonResponse.optString("body"),
                        state = jsonResponse.optString("state"),
                        url = jsonResponse.optString("html_url"),
                        createdAt = jsonResponse.optString("created_at")
                    )
                    
                    return@withContext GitHubResult(
                        success = true,
                        message = "تم إنشاء طلب السحب بنجاح",
                        data = pullRequest
                    )
                } else {
                    return@withContext GitHubResult(
                        success = false,
                        message = "فشل إنشاء طلب السحب: ${response.message}",
                        data = null
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating GitHub pull request", e)
            return@withContext GitHubResult(
                success = false,
                message = "فشل إنشاء طلب السحب: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * الحصول على قائمة طلبات السحب
     */
    suspend fun getPullRequests(token: String, repository: GitHubRepository): GitHubResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$API_URL/repos/${repository.fullName}/pulls")
                .header("Authorization", "token $token")
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(body)
                    
                    val pullRequests = mutableListOf<GitHubPullRequest>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        
                        val pullRequest = GitHubPullRequest(
                            id = json.optInt("id"),
                            number = json.optInt("number"),
                            title = json.optString("title"),
                            body = json.optString("body"),
                            state = json.optString("state"),
                            url = json.optString("html_url"),
                            createdAt = json.optString("created_at")
                        )
                        
                        pullRequests.add(pullRequest)
                    }
                    
                    return@withContext GitHubResult(
                        success = true,
                        message = "تم الحصول على قائمة طلبات السحب بنجاح",
                        data = pullRequests
                    )
                } else {
                    return@withContext GitHubResult(
                        success = false,
                        message = "فشل الحصول على قائمة طلبات السحب: ${response.message}",
                        data = null
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting GitHub pull requests", e)
            return@withContext GitHubResult(
                success = false,
                message = "فشل الحصول على قائمة طلبات السحب: ${e.message}",
                data = null
            )
        }
    }
    
    /**
     * الحصول على قائمة الفروع
     */
    suspend fun getBranches(token: String, repository: GitHubRepository): GitHubResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$API_URL/repos/${repository.fullName}/branches")
                .header("Authorization", "token $token")
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(body)
                    
                    val branches = mutableListOf<GitHubBranch>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        
                        val branch = GitHubBranch(
                            name = json.optString("name"),
                            protected = json.optBoolean("protected"),
                            commit = json.optJSONObject("commit")?.optString("sha") ?: ""
                        )
                        
                        branches.add(branch)
                    }
                    
                    return@withContext GitHubResult(
                        success = true,
                        message = "تم الحصول على قائمة الفروع بنجاح",
                        data = branches
                    )
                } else {
                    return@withContext GitHubResult(
                        success = false,
                        message = "فشل الحصول على قائمة الفروع: ${response.message}",
                        data = null
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting GitHub branches", e)
            return@withContext GitHubResult(
                success = false,
                message = "فشل الحصول على قائمة الفروع: ${e.message}",
                data = null
            )
        }
    }
}