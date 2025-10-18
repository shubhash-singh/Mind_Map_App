package com.ragnar.mindmaplearningapp.ui.components

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import com.ragnar.mindmaplearningapp.data.dataClass.GraphData
import com.ragnar.mindmaplearningapp.ui.theme.AccentBlue
import com.ragnar.mindmaplearningapp.ui.theme.BackgroundPrimary
import com.ragnar.mindmaplearningapp.ui.theme.BackgroundSecondary
import com.ragnar.mindmaplearningapp.ui.theme.TextPrimary
import com.ragnar.mindmaplearningapp.ui.theme.TextSecondary
import kotlinx.serialization.json.Json
import kotlin.math.hypot

@Composable
fun ConceptMapModel(json: String) {

    // Decode JSON with error handling - recomputes when json changes
    val graphData = remember(json) {
        try {
            Json.decodeFromString<GraphData>(json)
        } catch (e: Exception) {
            Log.e("ConceptMapModel", "Error parsing JSON", e)
            // Return default GraphData with proper structure
            createDefaultGraphData()
        }
    }

    // Paint objects for text rendering
    val titlePaint = remember {
        Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            textSize = 56f
            color = TextPrimary.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }
    }

    val textPaint = remember {
        Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            textSize = 28f
            color = TextPrimary.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    val edgeLabelPaint = remember {
        Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            textSize = 22f
            color = TextSecondary.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    // Color mapping for categories
    fun colorForCategory(category: String) = when (category) {
        "Core" -> Color(0xFF3B82F6)
        "Action" -> Color(0xFF10B981)
        "Outcome" -> Color(0xFFF59E0B)
        "Application" -> Color(0xFFEC4899)
        "Biological Feature" -> Color(0xFF8B5CF6)
        "Social Structure" -> Color(0xFF14B8A6)
        "History" -> Color(0xFF6366F1)
        "Biology" -> Color(0xFF10B981)
        "Digestive System" -> Color(0xFFF59E0B)
        "Nutrition" -> Color(0xFF84CC16)
        "Behavior" -> Color(0xFF06B6D4)
        "Health" -> Color(0xFFF43F5E)
        "Ecology" -> Color(0xFF22C55E)
        "Genetics" -> Color(0xFFA855F7)
        "Taxonomy" -> Color(0xFF3B82F6)
        "Agriculture" -> Color(0xFFF97316)
        else -> Color(0xFF64748B)
    }

    val nodeRadius = 90f

    // Reset positions when JSON changes
    val nodePositions = remember(json) { mutableStateMapOf<String, Offset>() }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val animatedScale by animateFloatAsState(
        targetValue = if (selectedNodeId != null) 1.15f else 1f,
        label = "node_scale_animation"
    )

    var canvasCenter by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundSecondary)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 3f)
                        offset += pan / scale
                    }
                }
                .pointerInput(scale, offset, canvasCenter) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val adjustedTouch = (down.position - offset - canvasCenter) / scale + canvasCenter

                        val touchedNodeId = nodePositions.entries.find { (_, pos) ->
                            adjustedTouch.getDistanceTo(pos) <= nodeRadius
                        }?.key

                        if (touchedNodeId != null) {
                            selectedNodeId = touchedNodeId

                            drag(down.id) { change ->
                                val dragAmount = change.positionChange()
                                nodePositions[touchedNodeId]?.let { currentPosition ->
                                    nodePositions[touchedNodeId] = currentPosition + (dragAmount / scale)
                                }
                                change.consume()
                            }

                            selectedNodeId = null
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            canvasCenter = center

            // Safety check - don't render if no nodes
            if (graphData.nodes.isEmpty()) {
                // Draw error message
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        "No data to display",
                        center.x,
                        center.y,
                        textPaint
                    )
                }
                return@Canvas
            }

            // Initialize hierarchical layout
            if (nodePositions.isEmpty()) {
                val totalNodes = graphData.nodes.size

                // Build adjacency list to find tree structure
                val adjacencyList = mutableMapOf<String, MutableList<String>>()
                val inDegree = mutableMapOf<String, Int>()

                graphData.nodes.forEach { node ->
                    adjacencyList[node.id] = mutableListOf()
                    inDegree[node.id] = 0
                }

                graphData.edges.forEach { edge ->
                    adjacencyList[edge.from]?.add(edge.to)
                    inDegree[edge.to] = (inDegree[edge.to] ?: 0) + 1
                }

                // Find root nodes (nodes with no incoming edges)
                val rootNodes = graphData.nodes.filter { (inDegree[it.id] ?: 0) == 0 }

                // If no clear roots, use first node (or return early if no nodes at all)
                if (graphData.nodes.isEmpty()) return@Canvas
                val roots = if (rootNodes.isEmpty()) listOf(graphData.nodes.first()) else rootNodes

                // Hierarchical layout parameters
                val startY = 150f
                val levelHeight = 400f
                val horizontalSpacing = 350f

                // BFS to assign levels
                val levels = mutableMapOf<String, Int>()
                val queue = ArrayDeque<Pair<String, Int>>()
                val visited = mutableSetOf<String>()

                roots.forEach { root ->
                    queue.add(root.id to 0)
                    visited.add(root.id)
                }

                while (queue.isNotEmpty()) {
                    val (nodeId, level) = queue.removeFirst()
                    levels[nodeId] = level

                    adjacencyList[nodeId]?.forEach { childId ->
                        if (childId !in visited) {
                            visited.add(childId)
                            queue.add(childId to level + 1)
                        }
                    }
                }

                // Assign remaining unvisited nodes
                graphData.nodes.forEach { node ->
                    if (node.id !in levels) {
                        levels[node.id] = 0
                    }
                }

                // Group nodes by level
                val nodesByLevel = levels.entries.groupBy({ it.value }, { it.key })
                val maxLevel = nodesByLevel.keys.maxOrNull() ?: 0

                // Position nodes in each level
                nodesByLevel.forEach { (level, nodesInLevel) ->
                    val y = startY + level * levelHeight
                    val levelWidth = (nodesInLevel.size - 1) * horizontalSpacing
                    val startX = center.x - levelWidth / 2

                    nodesInLevel.forEachIndexed { index, nodeId ->
                        val x = if (nodesInLevel.size == 1) {
                            center.x
                        } else {
                            startX + index * horizontalSpacing
                        }
                        nodePositions[nodeId] = Offset(x, y)
                    }
                }
            }

            withTransform({
                scale(scaleX = scale, scaleY = scale, pivot = center)
                translate(left = offset.x, top = offset.y)
            }) {

                // Draw edges with curved lines
                graphData.edges.forEach { edge ->
                    val fromPos = nodePositions[edge.from]
                    val toPos = nodePositions[edge.to]

                    if (fromPos != null && toPos != null) {
                        // Curved line using bezier
                        val path = Path().apply {
                            moveTo(fromPos.x, fromPos.y)
                            val controlX = (fromPos.x + toPos.x) / 2
                            val controlY = fromPos.y + (toPos.y - fromPos.y) * 0.3f
                            quadraticBezierTo(controlX, controlY, toPos.x, toPos.y)
                        }

                        drawPath(
                            path = path,
                            color = Color(0xFFD1D5DB),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                        )

                        // Edge label
                        val midX = (fromPos.x + toPos.x) / 2
                        val midY = (fromPos.y + toPos.y) / 2 - 20

                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(edge.label, midX, midY, edgeLabelPaint)
                        }
                    }
                }

                // Draw nodes
                graphData.nodes.forEach { node ->
                    val pos = nodePositions[node.id] ?: return@forEach

                    val color = if (selectedNodeId == node.id)
                        colorForCategory(node.category).copy(alpha = 0.9f)
                    else
                        colorForCategory(node.category)

                    val radius = nodeRadius * animatedScale

                    // Drop shadow
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.15f),
                        radius = radius,
                        center = pos + Offset(4f, 4f)
                    )

                    // Node circle
                    drawCircle(color = color, radius = radius, center = pos)

                    // Inner circle for depth
                    drawCircle(
                        color = Color.White.copy(alpha = 0.2f),
                        radius = radius * 0.85f,
                        center = pos
                    )

                    // Node label
                    drawMultiLineText(
                        text = node.label,
                        center = pos,
                        textPaint = textPaint,
                        maxWidth = radius * 1.6f
                    )
                }
            }

            // Draw title at the top (outside transform)
            drawIntoCanvas { canvas ->
                val titleY = 60f
                canvas.nativeCanvas.drawText(graphData.main_concept, center.x, titleY, titlePaint)
            }
        }

        // Zoom controls
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { scale = (scale * 1.2f).coerceIn(0.5f, 3f) },
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                containerColor = AccentBlue,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Zoom In",
                    tint = BackgroundPrimary
                )
            }

            Spacer(modifier = Modifier.size(8.dp))

            FloatingActionButton(
                onClick = { scale = (scale / 1.2f).coerceIn(0.5f, 3f) },
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                containerColor = Color(0xFF6366F1),
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Zoom Out",
                    tint = Color.White
                )
            }
        }
    }
}

// Helper function to create default GraphData
private fun createDefaultGraphData(): GraphData {
    // Parse a simple default JSON instead of constructing manually
    val defaultJson = """
    {
      "visualization_type": "Concept Map",
      "main_concept": "Loading...",
      "nodes": [
        {"id": "A", "label": "Concept", "category": "Core"}
      ],
      "edges": []
    }
    """.trimIndent()

    return try {
        Json.decodeFromString<GraphData>(defaultJson)
    } catch (e: Exception) {
        // If even this fails, the GraphData class structure is wrong
        throw IllegalStateException("GraphData class structure mismatch", e)
    }
}

// Helper functions
private fun DrawScope.drawMultiLineText(
    text: String,
    center: Offset,
    textPaint: android.graphics.Paint,
    maxWidth: Float
) {
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var currentLine = ""

    for (word in words) {
        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
        if (textPaint.measureText(testLine) > maxWidth) {
            if (currentLine.isNotEmpty()) lines.add(currentLine)
            currentLine = word
        } else {
            currentLine = testLine
        }
    }
    if (currentLine.isNotEmpty()) lines.add(currentLine)

    val fontMetrics = textPaint.fontMetrics
    val lineHeight = fontMetrics.descent - fontMetrics.ascent
    val totalHeight = lineHeight * lines.size
    val startY = center.y - totalHeight / 2 + lineHeight / 2

    drawIntoCanvas { canvas ->
        lines.forEachIndexed { index, line ->
            val y = startY + index * lineHeight - (fontMetrics.ascent + fontMetrics.descent) / 2
            canvas.nativeCanvas.drawText(line, center.x, y, textPaint)
        }
    }
}

private fun Offset.getDistanceTo(other: Offset): Float =
    hypot(x - other.x, y - other.y)