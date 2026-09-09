package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.data.NothingModesDatabase
import com.tdvorak.nothingmodes.data.entities.NotificationLogEntity
import com.tdvorak.nothingmodes.ui.theme.NothingCard
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingEmptyState
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class NotificationLogViewModel
    @Inject
    constructor(
        db: NothingModesDatabase,
    ) : ViewModel() {
        private val dao = db.notificationLogDao()

        val recent: StateFlow<List<NotificationLogEntity>> =
            dao.getRecentAsFlow(200).stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList(),
            )
    }

@Composable
fun NotificationLogScreen(
    onBack: () -> Unit,
    viewModel: NotificationLogViewModel = hiltViewModel(),
) {
    val entries by viewModel.recent.collectAsState()
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(title = "Notification Log", onBack = onBack)
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            if (entries.isEmpty()) {
                NothingEmptyState(
                    title = "No notifications logged",
                    description = "Posted notifications will appear here",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = NothingSpacing.md,
                            end = NothingSpacing.md,
                            top = NothingSpacing.lg,
                            bottom = NothingSpacing.xxxl,
                        ),
                ) {
                    item {
                        NothingSectionHeader(text = "Recent")
                        NothingCard {
                            entries.forEachIndexed { index, entry ->
                                if (index > 0) NothingDivider()
                                NothingListRow(
                                    title = entry.title.ifEmpty { entry.packageName },
                                    subtitle = listOfNotNull(entry.packageName, entry.text.takeIf { it.isNotEmpty() }).joinToString(" · "),
                                    trailing = {
                                        Text(
                                            text = dateFormat.format(Date(entry.postTime)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontFamily = NothingFonts.mono(),
                                        )
                                    },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(NothingSpacing.lg))
                    }
                }
            }
        }
    }
}
