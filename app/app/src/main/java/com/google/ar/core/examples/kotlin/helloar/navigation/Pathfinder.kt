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

import java.util.PriorityQueue

/**
 * A* over [NavigationGraph].
 *
 * Simple idea:
 * - Keep a priority queue of places to try next (lowest estimated total cost first).
 * - `g` = cost so far from the start along known edges.
 * - `h` = heuristic guess of remaining cost to the goal (0 = plain Dijkstra;
 *   Euclidean distance between live AR pins when available).
 * - `f = g + h` is what we sort by.
 * - When we pop the goal, reconstruct the path by walking `cameFrom` backward.
 */
object Pathfinder {
  data class PathResult(
    val waypointIds: List<String>,
    val totalCost: Float,
  )

  fun findPath(
    graph: NavigationGraph,
    startId: String,
    goalId: String,
    heuristic: (fromId: String, toId: String) -> Float = { _, _ -> 0f },
  ): PathResult? {
    if (startId == goalId) return PathResult(listOf(startId), 0f)

    data class Node(val id: String, val g: Float, val f: Float)

    val open = PriorityQueue<Node>(compareBy { it.f })
    val cameFrom = mutableMapOf<String, String>()
    val gScore = mutableMapOf(startId to 0f)
    open.add(Node(startId, 0f, heuristic(startId, goalId)))
    val closed = mutableSetOf<String>()

    while (open.isNotEmpty()) {
      val current = open.poll() ?: break
      if (current.id in closed) continue
      if (current.id == goalId) {
        return PathResult(reconstruct(cameFrom, goalId), current.g)
      }
      closed += current.id
      for ((neighbor, weight) in graph.neighbors(current.id)) {
        if (neighbor in closed) continue
        val tentative = current.g + weight
        if (tentative < (gScore[neighbor] ?: Float.POSITIVE_INFINITY)) {
          cameFrom[neighbor] = current.id
          gScore[neighbor] = tentative
          open.add(Node(neighbor, tentative, tentative + heuristic(neighbor, goalId)))
        }
      }
    }
    return null
  }

  private fun reconstruct(cameFrom: Map<String, String>, goalId: String): List<String> {
    val path = mutableListOf(goalId)
    var cur = goalId
    while (cameFrom.containsKey(cur)) {
      cur = cameFrom.getValue(cur)
      path.add(0, cur)
    }
    return path
  }
}
