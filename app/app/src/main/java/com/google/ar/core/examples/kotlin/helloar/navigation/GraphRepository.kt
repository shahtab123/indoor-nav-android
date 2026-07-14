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

import android.content.Context

class GraphRepository(private val context: Context) {
  @Volatile
  var graph: NavigationGraph = NavigationGraph()
    private set

  fun load() {
    graph = GraphPersistence.load(context)
  }

  fun save() {
    GraphPersistence.save(context, graph)
  }

  fun setGraph(newGraph: NavigationGraph) {
    graph = newGraph
    save()
  }

  fun connect(fromId: String, toId: String, weight: Float = 1f) {
    graph = graph.withEdge(fromId, toId, weight)
    save()
  }

  fun disconnect(fromId: String, toId: String) {
    graph = graph.withoutEdge(fromId, toId)
    save()
  }

  fun removeWaypoint(id: String) {
    graph = graph.withoutWaypoint(id)
    save()
  }

  fun clear() {
    graph = NavigationGraph()
    GraphPersistence.clear(context)
  }
}
