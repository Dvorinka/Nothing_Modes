package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tdvorak.nothingmodes.capabilities.CapabilitiesCache
import com.tdvorak.nothingmodes.capabilities.DeviceCapabilities
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.Condition
import com.tdvorak.nothingmodes.engine.model.EngineJson
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.model.actionDescription
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.engine.runtime.PrivacyScrubber
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.util.DisplayUnits
import com.tdvorak.nothingmodes.ui.util.rememberUnits
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Node-graph view of an automation: WHEN → ONLY IF → THEN → ON END lanes,
 * pannable and zoomable. Nodes are editable — tap opens the same config
 * sheets the builder uses; long-press deletes; "+" adds.
 */
@HiltViewModel
class AutomationGraphViewModel
    @Inject
    constructor(
        private val store: AutomationStore,
    ) : ViewModel() {
        private val _automation = MutableStateFlow<Automation?>(null)
        val automation = _automation.asStateFlow()

        fun load(id: String) {
            viewModelScope.launch { _automation.value = store.get(AutomationId(id)) }
        }

        fun update(transform: (Automation) -> Automation) {
            val current = _automation.value ?: return
            val next = transform(current)
            _automation.value = next
            viewModelScope.launch { store.save(next) }
        }
    }

private enum class Lane(val label: String) {
    WHEN("WHEN"),
    ONLY_IF("ONLY IF"),
    THEN("THEN"),
    ON_END("ON END"),
}

private sealed interface NodeKind {
    data object Trigger : NodeKind

    data class ConditionNode(val path: List<Int>) : NodeKind

    data class ActionNode(val index: Int, val end: Boolean) : NodeKind

    data class Add(val lane: Lane) : NodeKind
}

private data class GNode(
    val id: String,
    val lane: Lane,
    val row: Int,
    val indent: Int = 0,
    val title: String,
    val subtitle: String = "",
    val icon: ImageVector,
    val kind: NodeKind,
    val needsSetup: Boolean = false,
)

private data class GEdge(
    val from: String,
    val to: String,
    val dashed: Boolean = false,
)

private val NODE_W = 176.dp
private val NODE_H = 68.dp
private val LANE_W = 232.dp
private val ROW_H = 92.dp
private val INDENT = 24.dp
private const val HEADER_Y = 4f
private const val CONTENT_TOP = 36f

private data class Graph(
    val nodes: List<GNode>,
    val edges: List<GEdge>,
)

private fun buildGraph(
    automation: Automation,
    units: DisplayUnits,
    appLabel: (String) -> String,
): Graph {
    val nodes = mutableListOf<GNode>()
    val edges = mutableListOf<GEdge>()

    nodes +=
        GNode(
            id = "trigger",
            lane = Lane.WHEN,
            row = 0,
            title = "WHEN",
            subtitle = triggerDescription(automation.trigger, units, appLabel),
            icon = iconForTrigger(automation.trigger),
            kind = NodeKind.Trigger,
            needsSetup = PrivacyScrubber.missingFields(automation.trigger).isNotEmpty(),
        )
    nodes += addNode("add_when", Lane.WHEN, 1)

    // ONLY IF — flatten the condition tree; combinators get their own nodes.
    val condNodes = mutableListOf<Pair<List<Int>, Condition>>()
    automation.conditions?.let { flattenCondition(it, emptyList(), condNodes) }
    condNodes.forEachIndexed { i, (path, cond) ->
        nodes +=
            GNode(
                id = "cond_$i",
                lane = Lane.ONLY_IF,
                row = i,
                indent = path.size,
                title =
                    when (cond) {
                        is Condition.And -> "ALL OF"
                        is Condition.Or -> "ANY OF"
                        is Condition.Not -> "NOT"
                        else -> "IF"
                    },
                subtitle =
                    if (cond is Condition.And || cond is Condition.Or || cond is Condition.Not) {
                        ""
                    } else {
                        conditionDescription(cond, units)
                    },
                icon = iconForCondition(cond),
                kind = NodeKind.ConditionNode(path),
                needsSetup = PrivacyScrubber.missingFields(cond).isNotEmpty(),
            )
        // Combinator → child edge inside the lane.
        if (path.isNotEmpty()) {
            val parentIndex = condNodes.indexOfFirst { it.first == path.dropLast(1) }
            if (parentIndex >= 0) edges += GEdge("cond_$parentIndex", "cond_$i")
        }
    }
    nodes += addNode("add_onlyif", Lane.ONLY_IF, condNodes.size)

    automation.actions.forEachIndexed { i, action ->
        nodes +=
            GNode(
                id = "act_$i",
                lane = Lane.THEN,
                row = i,
                title = if (action is Action.Group) action.name.uppercase() else "THEN",
                subtitle = actionDescription(action),
                icon = iconForAction(action),
                kind = NodeKind.ActionNode(i, end = false),
                needsSetup = PrivacyScrubber.missingFields(action).isNotEmpty(),
            )
        if (i > 0) edges += GEdge("act_${i - 1}", "act_$i")
    }
    nodes += addNode("add_then", Lane.THEN, automation.actions.size)

    automation.endActions.forEachIndexed { i, action ->
        nodes +=
            GNode(
                id = "end_$i",
                lane = Lane.ON_END,
                row = i,
                title = "ON END",
                subtitle = actionDescription(action),
                icon = iconForAction(action),
                kind = NodeKind.ActionNode(i, end = true),
                needsSetup = PrivacyScrubber.missingFields(action).isNotEmpty(),
            )
        if (i > 0) edges += GEdge("end_${i - 1}", "end_$i")
    }
    nodes += addNode("add_end", Lane.ON_END, automation.endActions.size)

    // Lane chaining: WHEN → ONLY IF → THEN → ON END, skipping empty lanes.
    val firstCond = condNodes.indices.firstOrNull()?.let { "cond_$it" }
    val firstAction = automation.actions.indices.firstOrNull()?.let { "act_$it" }
    val firstEnd = automation.endActions.indices.firstOrNull()?.let { "end_$it" }
    var prev = "trigger"
    for (next in listOfNotNull(firstCond, firstAction, firstEnd)) {
        edges += GEdge(prev, next, dashed = next == firstEnd)
        prev = next
    }

    return Graph(nodes, edges)
}

private fun addNode(
    id: String,
    lane: Lane,
    row: Int,
) = GNode(id = id, lane = lane, row = row, title = "", icon = Icons.Outlined.Add, kind = NodeKind.Add(lane))

private fun flattenCondition(
    condition: Condition,
    path: List<Int>,
    out: MutableList<Pair<List<Int>, Condition>>,
) {
    out += path to condition
    when (condition) {
        is Condition.And -> condition.all.forEachIndexed { i, c -> flattenCondition(c, path + i, out) }
        is Condition.Or -> condition.any.forEachIndexed { i, c -> flattenCondition(c, path + i, out) }
        is Condition.Not -> flattenCondition(condition.cond, path + 0, out)
        else -> Unit
    }
}

private fun conditionAt(
    root: Condition?,
    path: List<Int>,
): Condition? {
    var current = root ?: return null
    for (i in path) {
        current =
            when (current) {
                is Condition.And -> current.all.getOrNull(i) ?: return null
                is Condition.Or -> current.any.getOrNull(i) ?: return null
                is Condition.Not -> if (i == 0) current.cond else return null
                else -> return null
            }
    }
    return current
}

private fun replaceConditionAt(
    root: Condition,
    path: List<Int>,
    new: Condition,
): Condition {
    if (path.isEmpty()) return new
    val head = path.first()
    val rest = path.drop(1)
    return when (root) {
        is Condition.And ->
            root.copy(all = root.all.mapIndexed { i, c -> if (i == head) replaceConditionAt(c, rest, new) else c })
        is Condition.Or ->
            root.copy(any = root.any.mapIndexed { i, c -> if (i == head) replaceConditionAt(c, rest, new) else c })
        is Condition.Not -> root.copy(cond = replaceConditionAt(root.cond, rest, new))
        else -> root
    }
}

private fun deleteConditionAt(
    root: Condition,
    path: List<Int>,
): Condition? {
    if (path.isEmpty()) return null
    val head = path.first()
    val rest = path.drop(1)
    return when (root) {
        is Condition.And -> {
            val kept = root.all.mapIndexedNotNull { i, c -> if (i == head) deleteConditionAt(c, rest) else c }
            when {
                kept.isEmpty() -> null
                kept.size == 1 -> kept[0] // collapse single-child AND
                else -> root.copy(all = kept)
            }
        }
        is Condition.Or -> {
            val kept = root.any.mapIndexedNotNull { i, c -> if (i == head) deleteConditionAt(c, rest) else c }
            when {
                kept.isEmpty() -> null
                kept.size == 1 -> kept[0]
                else -> root.copy(any = kept)
            }
        }
        is Condition.Not -> root.copy(cond = deleteConditionAt(root.cond, rest) ?: root.cond)
        else -> root
    }
}

@Composable
fun AutomationGraphScreen(
    automationId: String,
    onBack: () -> Unit,
    navController: NavController? = null,
    onConfigureTrigger: (String) -> Unit = {},
    onAddCondition: () -> Unit = {},
    onAddAction: () -> Unit = {},
    viewModel: AutomationGraphViewModel = hiltViewModel(),
) {
    LaunchedEffect(automationId) { viewModel.load(automationId) }
    val automation by viewModel.automation.collectAsState()
    val units = rememberUnits()
    val appLabel = rememberAppLabelResolver()
    val context = LocalContext.current
    var caps by remember { mutableStateOf(CapabilitiesCache.peek() ?: DeviceCapabilities()) }
    LaunchedEffect(Unit) { caps = CapabilitiesCache.refresh(context) }

    var userScale by remember { mutableFloatStateOf(0f) } // 0 = fit-to-width
    var pan by remember { mutableStateOf(Offset(24f, 12f)) }
    var editingAction by remember { mutableStateOf<Pair<NodeKind.ActionNode, Action>?>(null) }
    var editingCondition by remember { mutableStateOf<Pair<List<Int>, Condition>?>(null) }
    var pendingDelete by remember { mutableStateOf<GNode?>(null) }
    var pendingEndActions by remember { mutableStateOf(false) }

    // Results arriving back from config/catalog screens — same savedStateHandle
    // keys the simple builder uses.
    val backStackEntry by navController?.currentBackStackEntryAsState()
        ?: remember { mutableStateOf<NavBackStackEntry?>(null) }

    val triggerResultFlow =
        remember(backStackEntry) {
            backStackEntry?.savedStateHandle?.getStateFlow("trigger_result", "") ?: MutableStateFlow("")
        }
    val triggerResult by triggerResultFlow.collectAsStateWithLifecycle()
    LaunchedEffect(triggerResult, backStackEntry) {
        if (triggerResult.isNotEmpty()) {
            runCatching { Json.decodeFromString<Trigger>(triggerResult) }.getOrNull()?.let {
                viewModel.update { a -> a.copy(trigger = it) }
            }
            backStackEntry?.savedStateHandle?.set("trigger_result", "")
        }
    }

    val conditionsResultFlow =
        remember(backStackEntry) {
            backStackEntry?.savedStateHandle?.getStateFlow("condition_results", "") ?: MutableStateFlow("")
        }
    val conditionsResult by conditionsResultFlow.collectAsStateWithLifecycle()
    LaunchedEffect(conditionsResult, backStackEntry) {
        if (conditionsResult.isNotEmpty()) {
            runCatching { Json.decodeFromString<List<Condition>>(conditionsResult) }.getOrNull()
                ?.let { list ->
                    viewModel.update { a ->
                        val merged =
                            list.fold(a.conditions) { acc, c ->
                                if (acc == null) c else Condition.And(listOf(acc, c))
                            }
                        a.copy(conditions = merged)
                    }
                }
            backStackEntry?.savedStateHandle?.set("condition_results", "")
        }
    }

    val actionsResultFlow =
        remember(backStackEntry) {
            backStackEntry?.savedStateHandle?.getStateFlow("action_results", "") ?: MutableStateFlow("")
        }
    val actionsResult by actionsResultFlow.collectAsStateWithLifecycle()
    LaunchedEffect(actionsResult, backStackEntry) {
        if (actionsResult.isNotEmpty()) {
            runCatching { Json.decodeFromString<List<Action>>(actionsResult) }.getOrNull()
                ?.let { list ->
                    val toEnd = pendingEndActions
                    pendingEndActions = false
                    viewModel.update { a ->
                        if (toEnd) a.copy(endActions = a.endActions + list) else a.copy(actions = a.actions + list)
                    }
                }
            backStackEntry?.savedStateHandle?.set("action_results", "")
        }
    }

    val data = automation
    val graph = remember(data, units) { data?.let { buildGraph(it, units, appLabel) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = "GRAPH",
                onBack = onBack,
            )
        },
    ) { padding ->
        if (data == null || graph == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "[ LOADING... ]",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = NothingFonts.mono(),
                )
            }
            return@Scaffold
        }

        val lineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        val accentColor = NothingColors.accent
        val contentW = LANE_W * Lane.entries.size

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            val fitScale =
                (maxWidth.value / (contentW.value + 16f)).coerceIn(0.25f, 1f)
            val scale = if (userScale > 0f) userScale else fitScale

            // Gesture layer is an ancestor so it sees drags that start on nodes;
            // clicks still pass to the nodes' combinedClickable.
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, panDelta, zoom, _ ->
                                val base = if (userScale > 0f) userScale else fitScale
                                userScale = (base * zoom).coerceIn(0.25f, 2.5f)
                                pan += panDelta
                            }
                        },
            ) {
                // Content canvas in unscaled dp-space; graphicsLayer applies pan/zoom.
                // Children are offset-positioned and draw beyond the box bounds —
                // nothing clips them. (requiredSize centers oversized content —
                // don't use it here.)
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = pan.x,
                                translationY = pan.y,
                                transformOrigin = TransformOrigin(0f, 0f),
                            ),
                ) {
                val nodePos =
                    graph.nodes.associateBy({ it.id }) { node ->
                        Offset(
                            x = node.lane.ordinal * LANE_W.value + node.indent * INDENT.value,
                            y = CONTENT_TOP + node.row * ROW_H.value,
                        )
                    }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    graph.edges.forEach { edge ->
                        val a = nodePos[edge.from] ?: return@forEach
                        val b = nodePos[edge.to] ?: return@forEach
                        val start = Offset((a.x + NODE_W.value).dp.toPx(), (a.y + NODE_H.value / 2).dp.toPx())
                        val end = Offset(b.x.dp.toPx(), (b.y + NODE_H.value / 2).dp.toPx())
                        val color = if (edge.dashed) accentColor else lineColor
                        drawLine(
                            color = color,
                            start = start,
                            end = end,
                            strokeWidth = 2.dp.toPx(),
                            pathEffect =
                                if (edge.dashed) {
                                    PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                                } else {
                                    null
                                },
                        )
                        drawCircle(color = color, radius = 3.dp.toPx(), center = end)
                    }
                }

                Lane.entries.forEach { lane ->
                    Text(
                        text = lane.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = NothingFonts.mono(),
                        modifier =
                            Modifier.offset(
                                x = (lane.ordinal * LANE_W.value).dp,
                                y = HEADER_Y.dp,
                            ),
                    )
                }

                graph.nodes.forEach { node ->
                    val pos = nodePos[node.id] ?: return@forEach
                    GraphNodeBox(
                        node = node,
                        modifier = Modifier.offset(x = pos.x.dp, y = pos.y.dp),
                        onClick = {
                            when (val k = node.kind) {
                                is NodeKind.Trigger ->
                                    onConfigureTrigger(EngineJson.json.encodeToString(data.trigger))
                                is NodeKind.ConditionNode ->
                                    conditionAt(data.conditions, k.path)?.let { editingCondition = k.path to it }
                                is NodeKind.ActionNode -> {
                                    val list = if (k.end) data.endActions else data.actions
                                    list.getOrNull(k.index)?.let { editingAction = k to it }
                                }
                                is NodeKind.Add ->
                                    when (k.lane) {
                                        Lane.WHEN ->
                                            onConfigureTrigger(EngineJson.json.encodeToString(data.trigger))
                                        Lane.ONLY_IF -> onAddCondition()
                                        Lane.THEN -> {
                                            pendingEndActions = false
                                            onAddAction()
                                        }
                                        Lane.ON_END -> {
                                            pendingEndActions = true
                                            onAddAction()
                                        }
                                    }
                            }
                        },
                        onLongClick = {
                            if (node.kind is NodeKind.ActionNode || node.kind is NodeKind.ConditionNode) {
                                pendingDelete = node
                            }
                        },
                    )
                }
                }
            }
        }
    }

    editingAction?.let { (kind, action) ->
        ActionConfigSheet(
            action = action,
            onDone = { updated ->
                viewModel.update { a ->
                    val list = if (kind.end) a.endActions else a.actions
                    val next = list.toMutableList().also { it[kind.index] = updated }
                    if (kind.end) a.copy(endActions = next) else a.copy(actions = next)
                }
                editingAction = null
            },
            onDismiss = { editingAction = null },
            caps = caps,
        )
    }

    editingCondition?.let { (path, cond) ->
        ConditionConfigSheet(
            condition = cond,
            onDone = { updated ->
                viewModel.update { a ->
                    a.conditions?.let { a.copy(conditions = replaceConditionAt(it, path, updated)) } ?: a
                }
                editingCondition = null
            },
            onDismiss = { editingCondition = null },
        )
    }

    pendingDelete?.let { node ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (val k = node.kind) {
                            is NodeKind.ActionNode ->
                                viewModel.update { a ->
                                    if (k.end) {
                                        a.copy(endActions = a.endActions.filterIndexed { i, _ -> i != k.index })
                                    } else {
                                        a.copy(actions = a.actions.filterIndexed { i, _ -> i != k.index })
                                    }
                                }
                            is NodeKind.ConditionNode ->
                                viewModel.update { a ->
                                    a.copy(conditions = a.conditions?.let { deleteConditionAt(it, k.path) })
                                }
                            else -> Unit
                        }
                        pendingDelete = null
                    },
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text("Remove node?") },
            text = { Text(node.subtitle.ifBlank { node.title }) },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GraphNodeBox(
    node: GNode,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val isAdd = node.kind is NodeKind.Add
    Surface(
        shape = MaterialTheme.shapes.medium,
        color =
            if (isAdd) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    when {
                        node.needsSetup -> NothingColors.accent
                        isAdd -> MaterialTheme.colorScheme.outlineVariant
                        else -> MaterialTheme.colorScheme.outline
                    },
            ),
        modifier =
            modifier
                .size(NODE_W, NODE_H)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        if (isAdd) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Add",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    node.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = node.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = NothingFonts.mono(),
                            maxLines = 1,
                        )
                        if (node.needsSetup) {
                            Text(
                                text = "SETUP",
                                style = MaterialTheme.typography.labelSmall,
                                color = NothingColors.accent,
                                fontFamily = NothingFonts.mono(),
                                maxLines = 1,
                            )
                        }
                    }
                    if (node.subtitle.isNotBlank()) {
                        Text(
                            text = node.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = NothingFonts.mono(),
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}
