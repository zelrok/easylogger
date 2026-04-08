package com.easylogger.app.ui.blockrun

import android.media.AudioManager
import com.easylogger.app.ui.components.ScaleNumberInput
import android.media.ToneGenerator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import com.easylogger.app.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BlockRunScreen(
    onNavigateBack: () -> Unit,
    viewModel: BlockRunViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Audio ding via ToneGenerator
    val toneGenerator = remember {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
    }
    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BlockRunEvent.PlayDing -> toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
                is BlockRunEvent.RunCompleted -> { /* user taps Done to navigate back */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (state.items.isNotEmpty()) {
                        Text(
                            stringResource(
                                R.string.item_progress,
                                state.currentIndex + 1,
                                state.itemCount
                            )
                        )
                    } else {
                        Text(state.folderName)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        when (state.phase) {
            BlockRunPhase.SHOWING_ITEM -> ShowingItemContent(
                state = state,
                onLogNow = viewModel::logNow,
                onLogStart = viewModel::logStart,
                onLogStop = viewModel::logStop,
                onSubmitAnswer = viewModel::submitAnswer,
                onPause = viewModel::pauseTimer,
                onResume = viewModel::resumeTimer,
                onPauseAll = viewModel::pauseAll,
                onResumeAll = viewModel::resumeAll,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )

            BlockRunPhase.SHOWING_REST -> ShowingRestContent(
                state = state,
                onSkip = viewModel::skipRest,
                onPause = viewModel::pauseTimer,
                onResume = viewModel::resumeTimer,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )

            BlockRunPhase.COMPLETED -> CompletedContent(
                state = state,
                onDone = onNavigateBack,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShowingItemContent(
    state: BlockRunUiState,
    onLogNow: () -> Unit,
    onLogStart: () -> Unit,
    onLogStop: () -> Unit,
    onSubmitAnswer: (String) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = state.currentItem ?: return

    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Item name
        Text(
            text = item.name,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Timer display
        if (state.isTimed && state.totalSeconds > 0) {
            TimerDisplay(
                remainingSeconds = state.remainingSeconds,
                totalSeconds = state.totalSeconds,
                timerState = state.timerState,
                onPause = onPause,
                onResume = onResume,
                showControls = !state.hasOpenLogEntry
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action buttons
        when (item) {
            is BlockItem.CategoryBlockItem -> CategoryActions(
                hasOpenEntry = state.hasOpenLogEntry,
                logStartTimeMillis = state.logStartTimeMillis,
                totalPausedMillis = state.totalPausedMillis,
                elapsedPaused = state.elapsedPaused,
                onLogNow = onLogNow,
                onLogStart = onLogStart,
                onLogStop = onLogStop,
                onPauseAll = onPauseAll,
                onResumeAll = onResumeAll
            )

            is BlockItem.QuestionBlockItem -> QuestionActions(
                question = item.question,
                onSubmitAnswer = onSubmitAnswer
            )
        }
    }
}

@Composable
private fun TimerDisplay(
    remainingSeconds: Int,
    totalSeconds: Int,
    timerState: TimerState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    showControls: Boolean = true
) {
    val progress by animateFloatAsState(
        targetValue = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f,
        label = "timer_progress"
    )
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "%d:%02d".format(minutes, seconds),
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )

        if (!showControls) return@Column

        Spacer(modifier = Modifier.height(12.dp))

        when (timerState) {
            TimerState.RUNNING -> {
                IconButton(onClick = onPause, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Filled.Pause,
                        contentDescription = stringResource(R.string.pause),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            TimerState.PAUSED -> {
                IconButton(onClick = onResume, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.resume),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            TimerState.IDLE -> { /* no control shown */ }
        }
    }
}

@Composable
private fun CategoryActions(
    hasOpenEntry: Boolean,
    logStartTimeMillis: Long,
    totalPausedMillis: Long,
    elapsedPaused: Boolean,
    onLogNow: () -> Unit,
    onLogStart: () -> Unit,
    onLogStop: () -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit
) {
    val buttonShape = RoundedCornerShape(8.dp)
    val buttonModifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 72.dp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!hasOpenEntry) {
            Button(
                onClick = onLogNow,
                shape = buttonShape,
                modifier = buttonModifier
            ) {
                Text(stringResource(R.string.log_now), style = MaterialTheme.typography.titleLarge)
            }

            OutlinedButton(
                onClick = onLogStart,
                shape = buttonShape,
                modifier = buttonModifier
            ) {
                Text(stringResource(R.string.log_start), style = MaterialTheme.typography.titleLarge)
            }
        } else {
            // Elapsed timer counting up from log start
            ElapsedTimer(
                startTimeMillis = logStartTimeMillis,
                totalPausedMillis = totalPausedMillis,
                isPaused = elapsedPaused
            )

            // Pause/resume both countdown and elapsed timers
            if (!elapsedPaused) {
                OutlinedButton(
                    onClick = onPauseAll,
                    shape = buttonShape,
                    modifier = buttonModifier
                ) {
                    Icon(
                        Icons.Filled.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.pause), style = MaterialTheme.typography.titleLarge)
                }
            } else {
                OutlinedButton(
                    onClick = onResumeAll,
                    shape = buttonShape,
                    modifier = buttonModifier
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.resume), style = MaterialTheme.typography.titleLarge)
                }
            }

            FilledTonalButton(
                onClick = onLogStop,
                shape = buttonShape,
                modifier = buttonModifier
            ) {
                Text(stringResource(R.string.log_stop), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun ElapsedTimer(
    startTimeMillis: Long,
    totalPausedMillis: Long,
    isPaused: Boolean
) {
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(startTimeMillis, isPaused, totalPausedMillis) {
        if (startTimeMillis <= 0L) return@LaunchedEffect
        if (isPaused) {
            // Freeze at current elapsed value
            elapsedSeconds = (System.currentTimeMillis() - startTimeMillis - totalPausedMillis) / 1000
            return@LaunchedEffect
        }
        while (true) {
            elapsedSeconds = (System.currentTimeMillis() - startTimeMillis - totalPausedMillis) / 1000
            delay(1000)
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60

    Text(
        text = "%d:%02d".format(minutes, seconds),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionActions(
    question: com.easylogger.app.data.local.entity.Question,
    onSubmitAnswer: (String) -> Unit
) {
    val buttonShape = RoundedCornerShape(8.dp)

    if (question.answerType == "TEXT" && question.textOptions != null) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            question.textOptions.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                .forEach { option ->
                    Button(
                        onClick = { onSubmitAnswer(option) },
                        shape = buttonShape,
                        modifier = Modifier.heightIn(min = 64.dp)
                    ) {
                        Text(option, style = MaterialTheme.typography.titleLarge)
                    }
                }
        }
    } else if (question.scaleMax - question.scaleMin + 1 <= 20) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (i in question.scaleMin..question.scaleMax) {
                OutlinedButton(
                    onClick = { onSubmitAnswer(i.toString()) },
                    shape = buttonShape,
                    modifier = Modifier
                        .size(72.dp)
                ) {
                    Text(i.toString(), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    } else {
        ScaleNumberInput(
            scaleMin = question.scaleMin,
            scaleMax = question.scaleMax,
            onSubmit = { onSubmitAnswer(it) },
            buttonShape = buttonShape
        )
    }
}

@Composable
private fun ShowingRestContent(
    state: BlockRunUiState,
    onSkip: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.rest),
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (state.totalSeconds > 0) {
            TimerDisplay(
                remainingSeconds = state.remainingSeconds,
                totalSeconds = state.totalSeconds,
                timerState = state.timerState,
                onPause = onPause,
                onResume = onResume
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = onSkip,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
        ) {
            Icon(
                Icons.Filled.SkipNext,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.skip), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun CompletedContent(
    state: BlockRunUiState,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.block_complete),
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.item_progress, state.itemCount, state.itemCount),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDone,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
        ) {
            Text(stringResource(R.string.done), style = MaterialTheme.typography.titleLarge)
        }
    }
}
