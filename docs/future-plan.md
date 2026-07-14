# Indoor Spatial Platform (ISP)

**Author:** Shahtab | Licensed under [Apache 2.0](../LICENSE)

Future / parallel track to [IndoorNav](plan.md).  
**IndoorNav** = navigation product (this repo).  
**ISP** = self-hosted indoor localization platform (separate stack; integrates with IndoorNav in Phase 6).

---

## Status

| Phase      | Status                 |
| ---------- | ---------------------- |
| ✅ Phase 1A | Complete               |
| ⏳ Phase 1B | Benchmark & Validation |
| ⏳ Phase 2  | Android Scanner        |
| ⏳ Phase 3  | Map Builder            |
| ⏳ Phase 4  | API                    |
| ⏳ Phase 5  | Localization Engine    |
| ⏳ Phase 6  | IndoorNav Integration  |
| ⏳ Phase 7  | Native Deployment      |

---

# Phase 1A ✅

### IndoorNav Prototype

* ✅ Android project
* ✅ ARCore integration
* ✅ ARCore Cloud Anchors
* ✅ Waypoint creation
* ✅ Waypoint persistence
* ✅ Waypoint editing
* ✅ Navigation graph
* ✅ A* pathfinding
* ✅ 2D map prototype
* ✅ AR navigation prototype
* ✅ Cloud Anchor hosting
* ✅ Cloud Anchor resolving
* ✅ Scan Again flow
* ✅ Localization backend abstraction

### Research

* ✅ Indoor localization research
* ✅ Visual localization research
* ✅ Open-source survey
* ✅ OpenVPS analysis
* ✅ RTAB-Map analysis
* ✅ ORB-SLAM3 analysis
* ✅ HLoc analysis
* ✅ COLMAP analysis
* ✅ pycolmap analysis
* ✅ SuperPoint analysis
* ✅ LightGlue analysis
* ✅ PoseLib analysis
* ✅ EigenPlaces analysis
* ✅ FAISS analysis

### ISP Design

* ✅ Platform vision
* ✅ Architecture
* ✅ Core services
* ✅ Repository structure
* ✅ Plugin architecture
* ✅ Local coordinate system
* ✅ Hybrid localization design
* ✅ Failure recovery design
* ✅ Native deployment strategy
* ✅ Open-source reuse strategy
* ✅ Technology stack selection
* ✅ Benchmark protocol
* ✅ Development roadmap

---

# Phase 1B ⏳

* ☐ Install technology stack
* ☐ Build benchmark harness
* ☐ Benchmark retrieval
* ☐ Benchmark localization
* ☐ Benchmark feature database
* ☐ Benchmark apartment dataset
* ☐ Optimize pipeline
* ☐ Freeze implementation

---

# Final Technology Stack

| Layer                  | Selected            |
| ---------------------- | ------------------- |
| Android Tracking       | ARCore              |
| Camera                 | CameraX             |
| Reconstruction         | COLMAP / pycolmap   |
| Localization Framework | HLoc                |
| Image Retrieval        | EigenPlaces         |
| Feature Extraction     | SuperPoint          |
| Feature Matching       | LightGlue           |
| Pose Estimation        | PoseLib + OpenCV    |
| Image Processing       | OpenCV              |
| API                    | FastAPI             |
| Metadata Database      | PostgreSQL          |
| Feature Database       | FAISS               |
| Deployment             | Native Installation |

---

# Open Source Reuse

### Full

* COLMAP
* pycolmap
* HLoc
* OpenCV
* PoseLib
* FastAPI
* FAISS
* PostgreSQL

### Partial

* OpenVPS HLoc integration
* OpenVPS pycolmap integration
* OpenVPS localization concepts
* OpenVPS API concepts

### Not Used

* StrayScanner
* GeoPose
* OSM
* MapAligner
* iOS pipeline

---

# Core Services

* Data Acquisition
* Processing
* Map Builder
* Evaluation
* Feature Database
* Localization Engine
* Map Manager
* Storage
* API
* Dashboard

---

# Repository Structure

### IndoorNav

* Android App
* 2D Navigation
* AR Navigation
* SDK

### ISP

```text
isp/
├── acquisition/
├── processing/
├── map_builder/
├── evaluation/
├── localization/
├── map_manager/
├── storage/
├── api/
├── dashboard/
└── plugins/
```

---

# Plugin Architecture

* Reconstruction
* Image Retrieval
* Feature Extraction
* Feature Matching
* Pose Estimation
* Localization

---

# Roadmap

### ✅ Phase 1A

Research & Architecture

### ⏳ Phase 1B

Benchmark & Validation

### ⏳ Phase 2

Android Scanner

### ⏳ Phase 3

Map Builder

### ⏳ Phase 4

API

### ⏳ Phase 5

Localization Engine

### ⏳ Phase 6

IndoorNav Integration

### ⏳ Phase 7

Native Deployment
