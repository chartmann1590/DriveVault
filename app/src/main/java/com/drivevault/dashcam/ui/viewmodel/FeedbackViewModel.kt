package com.drivevault.dashcam.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drivevault.dashcam.data.feedback.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FeedbackUiState(
    val isConfigured: Boolean = false,
    val configError: String? = null,
    val bugReports: List<BugReport> = emptyList(),
    val showReportDialog: Boolean = false,
    val showIssueDetailDialog: Boolean = false,
    val selectedIssueNumber: Int = 0,
    val reportTitle: String = "",
    val reportDescription: String = "",
    val reportName: String = "",
    val reportEmail: String = "",
    val includeDiagnostics: Boolean = true,
    val selectedImageUri: Uri? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val submitSuccess: Boolean = false,
    val currentIssue: GithubIssue? = null,
    val comments: List<GithubComment> = emptyList(),
    val isLoadingIssue: Boolean = false,
    val issueError: String? = null,
    val commentText: String = "",
    val commentImageUri: Uri? = null,
    val isPostingComment: Boolean = false,
    val postCommentError: String? = null
)

class FeedbackViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = BugReportRepo(application)
    private val api = GithubApiClient

    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    init {
        checkConfig()
        viewModelScope.launch {
            repo.bugReports.collect { reports ->
                _uiState.update { it.copy(bugReports = reports) }
            }
        }
    }

    private fun checkConfig() {
        _uiState.update { it.copy(isConfigured = true, configError = null) }
    }

    fun showReportDialog() {
        _uiState.update {
            it.copy(
                showReportDialog = true,
                reportTitle = "",
                reportDescription = "",
                reportName = "",
                reportEmail = "",
                includeDiagnostics = true,
                selectedImageUri = null,
                isSubmitting = false,
                submitError = null,
                submitSuccess = false
            )
        }
    }

    fun hideReportDialog() {
        _uiState.update { it.copy(showReportDialog = false) }
    }

    fun setReportTitle(v: String) { _uiState.update { it.copy(reportTitle = v) } }
    fun setReportDescription(v: String) { _uiState.update { it.copy(reportDescription = v) } }
    fun setReportName(v: String) { _uiState.update { it.copy(reportName = v) } }
    fun setReportEmail(v: String) { _uiState.update { it.copy(reportEmail = v) } }
    fun setIncludeDiagnostics(v: Boolean) { _uiState.update { it.copy(includeDiagnostics = v) } }
    fun setSelectedImageUri(v: Uri?) { _uiState.update { it.copy(selectedImageUri = v) } }

    fun submitReport() {
        val state = _uiState.value
        if (state.reportTitle.isBlank() || state.reportDescription.isBlank()) return
        if (!state.isConfigured) return
        if (state.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }

            try {
                val context = getApplication<Application>()
                val bodyBuilder = StringBuilder()
                bodyBuilder.appendLine("## Description")
                bodyBuilder.appendLine()
                bodyBuilder.appendLine(state.reportDescription)
                bodyBuilder.appendLine()
                bodyBuilder.appendLine("## Contact Info")
                bodyBuilder.appendLine()
                bodyBuilder.appendLine("- Name: ${state.reportName.ifBlank { "Not provided" }}")
                bodyBuilder.appendLine("- Email: ${state.reportEmail.ifBlank { "Not provided" }}")

                var imageUrl: String? = null
                if (state.selectedImageUri != null) {
                    try {
                        val base64 = ImageUploadHelper.uriToBase64(context, state.selectedImageUri)
                        val filename = ImageUploadHelper.generateFilename()
                        val uploadResult = api.uploadAsset(filename, base64)
                        uploadResult.onSuccess { url ->
                            imageUrl = url
                        }.onFailure { e ->
                            _uiState.update { it.copy(isSubmitting = false, submitError = "Image upload failed: ${e.message}") }
                            return@launch
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(isSubmitting = false, submitError = "Image upload failed: ${e.message}") }
                        return@launch
                    }
                }

                if (imageUrl != null) {
                    bodyBuilder.appendLine()
                    bodyBuilder.appendLine("## Attachment")
                    bodyBuilder.appendLine()
                    bodyBuilder.appendLine("![Screenshot]($imageUrl)")
                }

                if (state.includeDiagnostics) {
                    bodyBuilder.appendLine()
                    bodyBuilder.appendLine(DiagnosticsHelper.collect(context))
                }

                val issueTitle = "[Feedback] ${state.reportTitle}"
                val result = api.createIssue(issueTitle, bodyBuilder.toString())
                result.onSuccess { issue ->
                    val report = BugReport(
                        number = issue.number,
                        title = issue.title,
                        status = issue.state,
                        createdAt = issue.createdAt,
                        htmlUrl = issue.htmlUrl
                    )
                    repo.saveBugReport(report)
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submitSuccess = true,
                            showReportDialog = false
                        )
                    }
                }.onFailure { e ->
                    _uiState.update { it.copy(isSubmitting = false, submitError = "Failed to create issue: ${e.message}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, submitError = "Unexpected error: ${e.message}") }
            }
        }
    }

    fun showIssueDetail(issueNumber: Int) {
        _uiState.update {
            it.copy(
                showIssueDetailDialog = true,
                selectedIssueNumber = issueNumber,
                currentIssue = null,
                comments = emptyList(),
                isLoadingIssue = true,
                issueError = null,
                commentText = "",
                commentImageUri = null,
                isPostingComment = false,
                postCommentError = null
            )
        }
        fetchIssueDetails(issueNumber)
    }

    fun hideIssueDetail() {
        _uiState.update { it.copy(showIssueDetailDialog = false) }
    }

    private fun fetchIssueDetails(issueNumber: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingIssue = true, issueError = null) }

            val issueResult = api.getIssue(issueNumber)
            issueResult.onSuccess { issue ->
                val updatedReport = BugReport(
                    number = issue.number,
                    title = issue.title,
                    status = issue.state,
                    createdAt = issue.createdAt,
                    htmlUrl = issue.htmlUrl
                )
                repo.saveBugReport(updatedReport)

                val commentsResult = api.getComments(issueNumber)
                commentsResult.onSuccess { comments ->
                    _uiState.update {
                        it.copy(
                            isLoadingIssue = false,
                            currentIssue = issue,
                            comments = comments
                        )
                    }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoadingIssue = false,
                            currentIssue = issue,
                            issueError = "Failed to load comments: ${e.message}"
                        )
                    }
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isLoadingIssue = false, issueError = "Failed to load issue: ${e.message}")
                }
            }
        }
    }

    fun setCommentText(v: String) { _uiState.update { it.copy(commentText = v) } }
    fun setCommentImageUri(v: Uri?) { _uiState.update { it.copy(commentImageUri = v) } }

    fun postComment() {
        val state = _uiState.value
        if (state.commentText.isBlank() && state.commentImageUri == null) return
        if (state.isPostingComment) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPostingComment = true, postCommentError = null) }

            try {
                val bodyBuilder = StringBuilder()
                if (state.commentText.isNotBlank()) {
                    bodyBuilder.appendLine("## Reply")
                    bodyBuilder.appendLine()
                    bodyBuilder.appendLine(state.commentText)
                }

                if (state.commentImageUri != null) {
                    val context = getApplication<Application>()
                    try {
                        val base64 = ImageUploadHelper.uriToBase64(context, state.commentImageUri)
                        val filename = ImageUploadHelper.generateFilename()
                        val uploadResult = api.uploadAsset(filename, base64)
                        uploadResult.onSuccess { url ->
                            bodyBuilder.appendLine()
                            bodyBuilder.appendLine("## Attachment")
                            bodyBuilder.appendLine()
                            bodyBuilder.appendLine("![Screenshot]($url)")
                        }.onFailure { e ->
                            _uiState.update {
                                it.copy(isPostingComment = false, postCommentError = "Image upload failed: ${e.message}")
                            }
                            return@launch
                        }
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(isPostingComment = false, postCommentError = "Image upload failed: ${e.message}")
                        }
                        return@launch
                    }
                }

                val result = api.postComment(state.selectedIssueNumber, bodyBuilder.toString())
                result.onSuccess {
                    _uiState.update {
                        it.copy(
                            isPostingComment = false,
                            commentText = "",
                            commentImageUri = null
                        )
                    }
                    fetchIssueDetails(state.selectedIssueNumber)
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(isPostingComment = false, postCommentError = "Failed to post comment: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isPostingComment = false, postCommentError = "Unexpected error: ${e.message}")
                }
            }
        }
    }

    fun refreshIssue() {
        val num = _uiState.value.selectedIssueNumber
        if (num > 0) fetchIssueDetails(num)
    }
}
