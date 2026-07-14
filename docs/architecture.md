# Architecture — IndoorNav + ISP

**Author:** Shahtab | Licensed under [Apache 2.0](../LICENSE)

## Two tracks

| Name | Role | Repo |
|------|------|------|
| **IndoorNav** | This app — waypoints, graph, A*, AR / later 2D + import | This repository |
| **ISP** (Indoor Spatial Platform) | Self-hosted indoor localization (scan → map → pose API) | Separate repository — see [future-plan.md](future-plan.md) |

**Product principle:** IndoorNav is the navigation product. Localization is a plug-in.  
**Today:** Google Cloud Anchors. **Later:** an **ISP client** behind `LocalizationBackend` (ISP Phase 6).

**Do not** wire OpenVPS, RTAB-Map, or ORB-SLAM3 into IndoorNav. ISP owns that stack (and may reuse concepts from Open-source tools — see future-plan).

---

## Rule

Google Cloud Anchors are **v1 only**. All IndoorNav layers talk to `LocalizationBackend`.  
Only `backend/GoogleCloudBackend.kt` may call ARCore Cloud Anchor host/resolve APIs.

```
IndoorNav UI / AR / Waypoints / A* / (later import)
                 │
         LocalizationBackend
                 │
        ┌────────┴────────┐
 GoogleCloudBackend    IspBackend (future)
      (v1 today)       talks to ISP server
```

---

## Packages (IndoorNav)

```
helloar/
  backend/     LocalizationBackend, GoogleCloudBackend, RTABMapBackend (legacy stub)
  waypoints/   Waypoint, WaypointRepository, WaypointPersistence
  navigation/  NavigationGraph, Pathfinder (A*), GraphPersistence
  settings/    AppSettings (backend kind, default floor)
  ar/          ArCoreTrackingToken, LiveAnchorRegistry
  ui/          AppMode, Compose models
```

---

## Swap Google Cloud Anchors → ISP later

When [future-plan.md](future-plan.md) reaches Phase 6:

1. Run the **ISP** server (map + localize API) from the ISP repo.
2. Add `IspBackend` (name TBD) implementing `LocalizationBackend` in IndoorNav.
3. Point it at the ISP API; set `AppSettings.localizationBackend` accordingly.
4. Keep A*, markers, Navigate UI — only pose source / host-resolve behavior changes.

`RTABMapBackend` is a leftover stub, not a planned product path. Prefer **ISP**.

IndoorNav ↔ ISP shape:

```
Phone (ARCore + CameraX frames)
        │
   IndoorNav IspBackend
        │  HTTP
   ISP Localization API
        │
      Pose → liveAnchors → AR / 2D nav
```

---

## Persistence (IndoorNav)

| File / store | Contents |
|--------------|----------|
| `waypoints.json` | id, name, type, backendAnchorId, floor, metadata |
| `nav_graph.json` | edges between waypoint ids |
| SharedPreferences | backend kind, default floor |

Later (IndoorNav plan Phases 3–5): internal apartment / floor-plan package for import/export.  
ISP stores maps and features in its own storage (PostgreSQL + FAISS — see future-plan).

---

## Related docs

- [plan.md](plan.md) — IndoorNav product roadmap  
- [future-plan.md](future-plan.md) — ISP phases, stack, services  
- [cloud-anchors.md](cloud-anchors.md) — v1 Cloud Anchors setup  
