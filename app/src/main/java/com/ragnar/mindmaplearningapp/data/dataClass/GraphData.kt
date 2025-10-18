package com.ragnar.mindmaplearningapp.data.dataClass

import kotlinx.serialization.Serializable

@Serializable
data class GraphData(
    val visualization_type: String,
    val main_concept: String,
    val nodes: List<Node>,
    val edges: List<Edge>
) {
    @Serializable
    data class Node(
        val id: String,
        val label: String,
        val category: String
    )

    @Serializable
    data class Edge(
        val from: String,
        val to: String,
        val label: String
    )
}