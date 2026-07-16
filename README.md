![Indoor Nav](banner.png)

# Indoor Nav

> **Experimental starter kit — not a finished or production-ready product.**

**Indoor Nav** is an open-source Android starter kit for building location-aware indoor AR experiences. The current prototype lets developers mark rooms and doors, connect them into a route graph, and test AR navigation.

It provides reusable foundations—AR tracking, spatial markers, route graphs, pathfinding, and localization—not just a single mapping app. One app currently has two modes, Map and Navigate. It uses Google’s ARCore `hello_ar_kotlin` sample as the rendering base.

- **App module:** [`app/`](app/) (`com.google.ar.core.examples.kotlin.helloar`)
- **Localization today:** Google Cloud Anchors (v1) — works, but limited (see below)
- **Next localization:** Indoor Spatial Platform (ISP) — [docs/future-plan.md](docs/future-plan.md)

---

## What you could build

The starter kit can be extended into experiences such as:

- **Indoor navigation:** Guide visitors through apartments, offices, campuses, museums, hospitals, or event venues.
- **Icebreaker scavenger hunt:** Hide virtual objects in different rooms. Players race to find them all, and the lowest completion time wins.
- **Treasure hunts and escape rooms:** Place clues, checkpoints, and puzzles at physical indoor locations.
- **Museum or property tours:** Reveal AR information when visitors reach an exhibit, room, or point of interest.
- **Training and onboarding:** Guide new employees through a workplace and attach instructions to equipment or locations.
- **Accessibility assistance:** Provide visual or audio guidance between important indoor destinations.

These are extension ideas, not features included in the current prototype.

---

## Limitations (why ISP)

This starter kit is experimental. Cloud Anchors are useful for prototyping, but not reliable enough for a finished everyday product:

- Pins can fail to resolve until you walk and rescan the room again
- Hosted anchors expire (~24h with an API key), so markers do not last
- Mapping feels fragile — lighting, featureless walls, and quotas all hurt quality

That is why the next major localization phase is **ISP** (Indoor Spatial Platform): a self-hosted indoor map / VPS-style backend behind the same `LocalizationBackend` plug-in, so **Indoor Nav** keeps the nav product and swaps out the weak cloud-anchor layer. Details: [docs/future-plan.md](docs/future-plan.md).

---

## Quick start

1. Phone + USB debugging → [docs/setup.md](docs/setup.md)

2. **API key** (required for saving markers to the cloud):
   1. [Create your own Google Cloud project](https://console.cloud.google.com/projectcreate) and select it.
   2. Enable [ARCore API](https://console.cloud.google.com/apis/library/arcore.googleapis.com) in that project.
   3. [Create an API key](https://console.cloud.google.com/apis/credentials) (Credentials → Create credentials → API key).
   4. Open `app/local.properties` and add (no quotes, no spaces around `=`):

```properties
ARCORE_API_KEY=paste_your_key_here
```

   Full steps (restrict key, billing, troubleshooting): [docs/cloud-anchors.md](docs/cloud-anchors.md)  
   Do **not** commit `local.properties` — it is gitignored.

3. Install:

```powershell
cd "d:\indoor map\app"
.\gradlew.bat installDebug
```

4. Map mode → mark spots (Wi‑Fi on) → wait for cloud host success → reopen and walk rooms to resolve pins

How to walk and mark your home: [docs/phone-capture.md](docs/phone-capture.md)

---

## Documentation

All project docs live in [`docs/`](docs/README.md).

| Doc | What it’s for |
|-----|----------------|
| [docs/plan.md](docs/plan.md) | Indoor Nav product roadmap |
| [docs/future-plan.md](docs/future-plan.md) | ISP / IndoorVPS track |
| [docs/architecture.md](docs/architecture.md) | Localization plug-in |
| [docs/cloud-anchors.md](docs/cloud-anchors.md) | API key & quotas |
| [docs/setup.md](docs/setup.md) | Phone setup & install |
| [docs/phone-capture.md](docs/phone-capture.md) | Mapping an apartment |

Upstream ARCore SDK (reference): [google-ar/arcore-android-sdk](https://github.com/google-ar/arcore-android-sdk)

---

## Architecture (short)

```
Apartment import (future) → Internal map → Waypoints → Graph → A*
                              │
                    ┌─────────┴─────────┐
                    │                   │
               2D Navigation      AR Navigation
                    │                   │
                    └─────────┬─────────┘
                              │
                       LocalizationBackend
                              │
              Cloud Anchors (today) | ISP client (future)
```

Details: [docs/architecture.md](docs/architecture.md) · phases: [docs/plan.md](docs/plan.md)

**Do not** commit API keys or `client_secret*.json`. Put the key only in `app/local.properties` as `ARCORE_API_KEY=…` (gitignored).

---

## Current status

**Prototype core complete:** AR Map/Navigate (no 2D floor plan yet), waypoints + save/load, A* + connections, Cloud Anchors, and path cues.

This does **not** mean the product is finished. It still needs stronger localization, testing, security review, onboarding, accessibility work, and production UX.

**Next:** Phase 2 — navigation platform. See [docs/plan.md](docs/plan.md).  
**ISP:** better localization so users stop depending on constant rescans — [docs/future-plan.md](docs/future-plan.md).  
**Not building in this repo:** CAD editor, OpenVPS/RTAB/ORB-SLAM.

---

## License

Copyright 2026 **Shahtab**. Indoor Nav is licensed under the [Apache License 2.0](LICENSE). See [NOTICE](NOTICE) for Google ARCore sample attribution.

Upstream ARCore SDK: [google-ar/arcore-android-sdk](https://github.com/google-ar/arcore-android-sdk).
