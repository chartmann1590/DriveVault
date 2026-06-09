package com.drivevault.dashcam.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.drivevault.dashcam.data.feedback.BugReport
import com.drivevault.dashcam.data.feedback.GithubComment
import com.drivevault.dashcam.ui.theme.*
import com.drivevault.dashcam.ui.viewmodel.FeedbackUiState
import com.drivevault.dashcam.ui.viewmodel.FeedbackViewModel

@Composable
fun SupportFeedbackSection(
    viewModel: FeedbackViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(8.dp))

        if (state.configError != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SafetyRedContainer),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = SafetyRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.configError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = SafetyRed
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = { viewModel.showReportDialog() },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.isConfigured,
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricBlue,
                disabledContainerColor = OutlineVariant
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Filled.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Report a Problem")
        }

        if (state.bugReports.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Submitted Reports",
                style = MaterialTheme.typography.labelLarge,
                color = OnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            state.bugReports.forEach { report ->
                BugReportRow(
                    report = report,
                    onClick = { viewModel.showIssueDetail(report.number) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        if (state.showReportDialog) {
            ReportProblemDialog(
                state = state,
                onTitleChange = viewModel::setReportTitle,
                onDescriptionChange = viewModel::setReportDescription,
                onNameChange = viewModel::setReportName,
                onEmailChange = viewModel::setReportEmail,
                onIncludeDiagnosticsChange = viewModel::setIncludeDiagnostics,
                onImageSelected = viewModel::setSelectedImageUri,
                onSubmit = viewModel::submitReport,
                onDismiss = viewModel::hideReportDialog
            )
        }

        if (state.showIssueDetailDialog) {
            IssueDetailDialog(
                state = state,
                onCommentTextChange = viewModel::setCommentText,
                onCommentImageSelected = viewModel::setCommentImageUri,
                onPostComment = viewModel::postComment,
                onRefresh = viewModel::refreshIssue,
                onDismiss = viewModel::hideIssueDetail
            )
        }
    }
}

@Composable
private fun BugReportRow(
    report: BugReport,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "#${report.number} - ${report.createdAt.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            val isOpen = report.status.equals("open", ignoreCase = true)
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isOpen) SuccessGreen.copy(alpha = 0.2f) else SafetyRed.copy(alpha = 0.2f)
            ) {
                Text(
                    text = report.status.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOpen) SuccessGreen else SafetyRed,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportProblemDialog(
    state: FeedbackUiState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onIncludeDiagnosticsChange: (Boolean) -> Unit,
    onImageSelected: (Uri?) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> onImageSelected(uri) }

    AlertDialog(
        onDismissRequest = { if (!state.isSubmitting) onDismiss() },
        title = { Text("Report a Problem", color = OnSurface) },
        containerColor = SurfaceContainer,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AmberWarning.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Your report will be submitted to this app's GitHub issue tracker. " +
                            "Do not include passwords, private keys, medical information, financial " +
                            "information, or anything you do not want visible to the repository " +
                            "maintainers. If this repository is public, your report may be publicly visible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberWarning,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                if (state.selectedImageUri != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AmberWarning.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Screenshots may contain private information. Review before submitting.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AmberWarning,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = state.reportTitle,
                    onValueChange = onTitleChange,
                    label = { Text("Title / Subject *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = OutlineVariant
                    )
                )

                OutlinedTextField(
                    value = state.reportDescription,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 150.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = OutlineVariant
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.includeDiagnostics,
                        onCheckedChange = onIncludeDiagnosticsChange,
                        colors = CheckboxDefaults.colors(checkedColor = ElectricBlue)
                    )
                    Text("Include diagnostics", style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedTextField(
                        value = state.reportName,
                        onValueChange = onNameChange,
                        label = { Text("Name (optional)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface,
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = OutlineVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = state.reportEmail,
                        onValueChange = onEmailChange,
                        label = { Text("Email (optional)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface,
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = OutlineVariant
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            imageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Attach Image")
                    }
                    if (state.selectedImageUri != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { onImageSelected(null) }) {
                            Icon(Icons.Filled.Clear, "Remove", tint = SafetyRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                if (state.selectedImageUri != null) {
                    AsyncImage(
                        model = state.selectedImageUri,
                        contentDescription = "Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Fit
                    )
                }

                if (state.submitError != null) {
                    Text(
                        text = state.submitError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = SafetyRed
                    )
                }

                if (state.isSubmitting) {
                    Text(
                        text = "Submitting...",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricBlue
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = state.reportTitle.isNotBlank()
                    && state.reportDescription.isNotBlank()
                    && !state.isSubmitting
                    && state.isConfigured,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !state.isSubmitting
            ) {
                Text("Cancel", color = OnSurfaceVariant)
            }
        }
    )
}

@Composable
private fun IssueDetailDialog(
    state: FeedbackUiState,
    onCommentTextChange: (String) -> Unit,
    onCommentImageSelected: (Uri?) -> Unit,
    onPostComment: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val commentImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> onCommentImageSelected(uri) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.isLoadingIssue) {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (state.currentIssue != null) "#${state.currentIssue.number}" else "Loading...",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface
                )
            }
        },
        containerColor = SurfaceContainer,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.issueError != null) {
                    Text(
                        text = state.issueError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = SafetyRed
                    )
                }

                state.currentIssue?.let { issue ->
                    Text(
                        text = issue.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isOpen = issue.state.equals("open", ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isOpen) SuccessGreen.copy(alpha = 0.2f) else SafetyRed.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = issue.state.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOpen) SuccessGreen else SafetyRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = issue.createdAt.take(10),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }

                    HorizontalDivider(color = OutlineVariant)

                    if (state.comments.isEmpty() && !state.isLoadingIssue) {
                        Text(
                            text = "No comments yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(state.comments) { comment ->
                                CommentItem(comment = comment)
                            }
                        }
                    }

                    HorizontalDivider(color = OutlineVariant)

                    OutlinedTextField(
                        value = state.commentText,
                        onValueChange = onCommentTextChange,
                        label = { Text("Write a reply...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface,
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = OutlineVariant
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                commentImageLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Icon(Icons.Filled.Image, null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Attach", style = MaterialTheme.typography.labelMedium)
                        }
                        if (state.commentImageUri != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = { onCommentImageSelected(null) }) {
                                Icon(Icons.Filled.Clear, "Remove", tint = SafetyRed, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = onPostComment,
                            enabled = (state.commentText.isNotBlank() || state.commentImageUri != null)
                                && !state.isPostingComment,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            if (state.isPostingComment) {
                                Text("...", style = MaterialTheme.typography.labelMedium, color = DeepCharcoal)
                            } else {
                                Text("Reply", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    if (state.postCommentError != null) {
                        Text(
                            text = state.postCommentError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = SafetyRed
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Refresh", color = ElectricBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = OnSurfaceVariant)
            }
        }
    )
}

@Composable
private fun CommentItem(comment: GithubComment) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = comment.user.login,
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricBlue,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = comment.createdAt.take(10),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment.body.take(500),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurface
            )
            if (comment.body.length > 500) {
                Text(
                    text = "...",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}
