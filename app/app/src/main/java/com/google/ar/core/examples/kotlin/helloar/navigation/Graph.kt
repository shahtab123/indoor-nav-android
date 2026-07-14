/*
 * Copyright 2026 Shahtab
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.examples.kotlin.helloar.navigation

/**
 * Undirected navigation graph between waypoint ids.
 * Completely independent of localization backend / Google.
 *
 * Users define which rooms/doors connect (e.g. Entrance ↔ Hallway).
 * [Pathfinder] walks these edges to plan a route for Navigate mode.
 */
data class GraphEdge(
  val fromId: String,
  val toId: String,
  /** Optional weight in meters; default 1 when poses are unknown. */
  val weight: Float = 1f,
)

data class NavigationGraph(
  val edges: List<GraphEdge> = emptyList(),
) {
  fun neighbors(id: String): List<Pair<String, Float>> {
    val out = mutableListOf<Pair<String, Float>>()
    for (e in edges) {
      when (id) {
        e.fromId -> out += e.toId to e.weight
        e.toId -> out += e.fromId to e.weight
      }
    }
    return out
  }

  fun withEdge(fromId: String, toId: String, weight: Float = 1f): NavigationGraph {
    if (fromId == toId) return this
    if (hasEdge(fromId, toId)) return this
    return copy(edges = edges + GraphEdge(fromId, toId, weight))
  }

  fun hasEdge(fromId: String, toId: String): Boolean =
    edges.any {
      (it.fromId == fromId && it.toId == toId) || (it.fromId == toId && it.toId == fromId)
    }

  fun withoutEdge(fromId: String, toId: String): NavigationGraph =
    copy(
      edges =
        edges.filterNot {
          (it.fromId == fromId && it.toId == toId) || (it.fromId == toId && it.toId == fromId)
        },
    )

  fun withoutWaypoint(id: String): NavigationGraph =
    copy(edges = edges.filter { it.fromId != id && it.toId != id })
}
