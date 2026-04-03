package com.smartamenities.data.graph

/**
 * Terminal D floor plan graph — used for MAP DRAWING only.
 * Routing math (Dijkstra's shortest path) runs in the Python backend.
 *
 * Coordinates are normalized 0.0–1.0 relative to the map canvas:
 *   x = 0.0 is west end, x = 1.0 is east end
 *   y = 0.0 is top (north wall gates), y = 1.0 is bottom (south wall gates)
 */

data class GraphNode(val id: String, val x: Float, val y: Float)
data class GraphEdge(val from: String, val to: String, val walkSeconds: Int)

object TerminalDGraph {

    val nodes = listOf(
        // Gates top wall D5–D22 (y = 0.22f, evenly spaced x from 0.04 to 0.96)
        GraphNode("D5",  0.04f, 0.22f),
        GraphNode("D6",  0.10f, 0.22f),
        GraphNode("D7",  0.16f, 0.22f),
        GraphNode("D8",  0.22f, 0.22f),
        GraphNode("D9",  0.28f, 0.22f),
        GraphNode("D10", 0.34f, 0.22f),
        GraphNode("D11", 0.40f, 0.22f),
        GraphNode("D12", 0.46f, 0.22f),
        GraphNode("D14", 0.52f, 0.22f),
        GraphNode("D15", 0.58f, 0.22f),
        GraphNode("D16", 0.64f, 0.22f),
        GraphNode("D17", 0.70f, 0.22f),
        GraphNode("D18", 0.76f, 0.22f),
        GraphNode("D19", 0.82f, 0.22f),
        GraphNode("D20", 0.88f, 0.22f),
        GraphNode("D21", 0.92f, 0.22f),
        GraphNode("D22", 0.96f, 0.22f),

        // Gates bottom wall D23–D40 (y = 0.78f, evenly spaced x from 0.96 to 0.04)
        GraphNode("D23", 0.96f, 0.78f),
        GraphNode("D24", 0.90f, 0.78f),
        GraphNode("D25", 0.84f, 0.78f),
        GraphNode("D26", 0.78f, 0.78f),
        GraphNode("D27", 0.72f, 0.78f),
        GraphNode("D28", 0.66f, 0.78f),
        GraphNode("D29", 0.60f, 0.78f),
        GraphNode("D30", 0.54f, 0.78f),
        GraphNode("D31", 0.48f, 0.78f),
        GraphNode("D33", 0.42f, 0.78f),
        GraphNode("D34", 0.36f, 0.78f),
        GraphNode("D36", 0.30f, 0.78f),
        GraphNode("D37", 0.24f, 0.78f),
        GraphNode("D38", 0.18f, 0.78f),
        GraphNode("D39", 0.12f, 0.78f),
        GraphNode("D40", 0.06f, 0.78f),

        // Corridor intersections (y = 0.50f, horizontal spine)
        GraphNode("COR_W", 0.04f, 0.50f),
        GraphNode("COR_C", 0.50f, 0.50f),
        GraphNode("COR_E", 0.96f, 0.50f),

        // Skylink stations
        GraphNode("SKY_W", 0.38f, 0.50f),
        GraphNode("SKY_E", 0.62f, 0.50f),

        // Security checkpoints
        GraphNode("SEC_D18", 0.76f, 0.50f),
        GraphNode("SEC_D30", 0.54f, 0.50f),

        // Restroom nodes (leaf nodes, slightly offset from gate positions)
        GraphNode("REST_D6",  0.10f, 0.35f),
        GraphNode("REST_D10", 0.34f, 0.35f),
        GraphNode("REST_D17", 0.70f, 0.35f),
        GraphNode("REST_D20", 0.88f, 0.35f),
        GraphNode("REST_D22", 0.96f, 0.35f),
        GraphNode("REST_D24", 0.90f, 0.65f),
        GraphNode("REST_D27", 0.72f, 0.65f),
        GraphNode("REST_D29", 0.60f, 0.65f),
        GraphNode("REST_D36", 0.30f, 0.65f),
        GraphNode("REST_D40", 0.06f, 0.65f),
        GraphNode("FAM_D18",  0.76f, 0.35f),
        GraphNode("FAM_D25",  0.84f, 0.65f),
        GraphNode("FAM_D28",  0.66f, 0.65f),
        GraphNode("LAC_D22",  0.96f, 0.38f),
    )

    val edges = listOf(
        // Corridor spine (21 seconds between adjacent nodes ~30m apart)
        GraphEdge("COR_W", "D5",    21),
        GraphEdge("D5",    "D6",    21),
        GraphEdge("D6",    "D7",    21),
        GraphEdge("D7",    "D8",    21),
        GraphEdge("D8",    "D9",    21),
        GraphEdge("D9",    "D10",   21),
        GraphEdge("D10",   "D11",   21),
        GraphEdge("D11",   "SKY_W", 21),
        GraphEdge("SKY_W", "D12",   21),
        GraphEdge("D12",   "D14",   21),
        GraphEdge("D14",   "D15",   21),
        GraphEdge("D15",   "D16",   21),
        GraphEdge("D16",   "D17",   21),
        GraphEdge("D17",   "D18",   21),
        GraphEdge("D18",   "SEC_D18", 10),
        GraphEdge("SEC_D18", "D19", 10),
        GraphEdge("D19",   "D20",   21),
        GraphEdge("D20",   "D21",   21),
        GraphEdge("D21",   "D22",   21),
        GraphEdge("D22",   "COR_E", 15),

        // Bottom wall corridor
        GraphEdge("COR_E", "D23",   15),
        GraphEdge("D23",   "D24",   21),
        GraphEdge("D24",   "D25",   21),
        GraphEdge("D25",   "D26",   21),
        GraphEdge("D26",   "D27",   21),
        GraphEdge("D27",   "D28",   21),
        GraphEdge("D28",   "D29",   21),
        GraphEdge("D29",   "SEC_D30", 10),
        GraphEdge("SEC_D30", "D30", 10),
        GraphEdge("D30",   "D31",   21),
        GraphEdge("D31",   "SKY_E", 21),
        GraphEdge("SKY_E", "D33",   21),
        GraphEdge("D33",   "D34",   21),
        GraphEdge("D34",   "D36",   21),
        GraphEdge("D36",   "D37",   21),
        GraphEdge("D37",   "D38",   21),
        GraphEdge("D38",   "D39",   21),
        GraphEdge("D39",   "D40",   21),
        GraphEdge("D40",   "COR_W", 15),

        // Cross corridor connections (west to east)
        GraphEdge("COR_W", "COR_C", 120),
        GraphEdge("COR_C", "COR_E", 120),

        // Restroom leaf edges (gate to restroom, ~10-15 seconds)
        GraphEdge("D6",  "REST_D6",  10),
        GraphEdge("D10", "REST_D10", 10),
        GraphEdge("D17", "REST_D17", 10),
        GraphEdge("D20", "REST_D20", 10),
        GraphEdge("D22", "REST_D22", 10),
        GraphEdge("D24", "REST_D24", 10),
        GraphEdge("D27", "REST_D27", 10),
        GraphEdge("D29", "REST_D29", 10),
        GraphEdge("D36", "REST_D36", 10),
        GraphEdge("D40", "REST_D40", 10),
        GraphEdge("D18", "FAM_D18",  12),
        GraphEdge("D25", "FAM_D25",  12),
        GraphEdge("D28", "FAM_D28",  12),
        GraphEdge("D22", "LAC_D22",  12),
    )

    // Convenience: set of all restroom-type node IDs for the routing API
    val amenityNodeIds = setOf(
        "REST_D6", "REST_D10", "REST_D17", "REST_D20", "REST_D22",
        "REST_D24", "REST_D27", "REST_D29", "REST_D36", "REST_D40",
        "FAM_D18", "FAM_D25", "FAM_D28", "LAC_D22"
    )
}
