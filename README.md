# 🧰 Sensor ToolBox — Advanced Sensor & Navigation Suite

## 📱 Mobile Application Development (Assignment 6)

A feature-complete, modern Android application showcasing **16+ sensor-driven utilities, geodetic inspection tools, and interactive OpenStreetMap navigation systems** built natively with Kotlin, Google Play Services, Material Design 3, and osmdroid.

---

## 🌟 What's New in Assignment 6

| Feature Area | Key Enhancements |
| :--- | :--- |
| 🗺️ **Map Location (`MapActivity`)** | **OpenStreetMap Nominatim Landmark Geocoding** (resolves POIs like *CN Tower* to exact coordinates), **OSRM Street Routing Engine**, **20 FPS Animated Traveling Beacon Pulse** along road polylines, **Multi-Layer Map Switcher** (Mapnik Standard, OpenTopoMap, HikeBike), **Compass Azimuth Lock** using hardware rotation vectors, and **Astronomical Solar Ray Vectors** (Sunrise, Live Sun, Sunset). |
| 📡 **Live Location (`LiveLocationActivity`)** | **Turn-by-Turn Road Navigation Cockpit** with dynamic maneuver icons (`⬆️`, `↗️`, `↖️`, `➡️`, `⬅️`, `🏁`), next-turn distance countdowns, street instructions, **Follow-Me Auto-Tracking Camera**, and **Live Geodetic Telemetry** (DMS coordinates, elevation, speed, accuracy badge). |
| 🏃 **Fitness Tracker (`FitnessTrackerActivity`)** | Live workout telemetry measuring velocity, cumulative geodesic distance, **running pace (min/km)**, **MET energy expenditure (calories burned)**, duration timer, and breadcrumb route mapping on OpenStreetMap. |
| 🥾 **Trail Tracker (`TrailTrackerActivity`)** | Enhanced hiking trail recorder with real-time polyline rendering on OpenStreetMap/OpenTopoMap, start/end pins, elevation gain/loss, and one-tap trail sharing. |
| ⚡ **Modernized Sensor Architecture** | 100% updated to modern, non-deprecated Android APIs (`TYPE_ROTATION_VECTOR`, `TYPE_GEOMAGNETIC_ROTATION_VECTOR`, and `CustomZoomButtonsController`). |

---

## 📊 Comprehensive Tools Directory

### 📍 Location & Navigation

| Icon | Tool Name | Sensors / APIs | Description & Key Features |
| :---: | :--- | :--- | :--- |
| 🗺️ | **Map Location** | GPS + OpenStreetMap + OSRM | Full-screen interactive map with place search, multi-layer tiles, animated route pulse, azimuth lock, and solar rays. |
| 📡 | **Live Location** | GPS + OpenStreetMap + Geocoder | Google Maps-style navigation with turn maneuvers, step countdowns, and real-time geodetic telemetry. |
| 📍 | **GPS Location** | FusedLocationProviderClient | Precise latitude/longitude in Decimal and DMS (`DD° MM' SS" N/W`), accuracy in meters, and reverse geocoded address. |
| 🥾 | **Trail Tracker** | GPS + OpenStreetMap | Records hiking trails with live breadcrumb polyline, duration timer, distance, and elevation gain. |
| 🧭 | **Digital Compass** | Accelerometer + Magnetometer | Tilt-compensated compass with 0–360° azimuth, cardinal directions, and one-tap heading lock on map. |

### 📊 Speed & Distance

| Icon | Tool Name | Sensors / APIs | Description & Key Features |
| :---: | :--- | :--- | :--- |
| 🏎️ | **Speedometer** | GPS | Instantaneous velocity in km/h and mph, gauge indicator, trip maximum, and average speed. |
| 📏 | **Distance Tracker** | GPS | Odometer measuring cumulative geodesic distance with active trip duration timer and speed averages. |
| ⛰️ | **Altimeter** | GPS Altitude | Altitude above sea level in meters, minimum/maximum elevation reached, cumulative ascent gain, and descent loss. |
| 🏃 | **Fitness Tracker** | GPS + MET Formulas | Workout telemetry computing speed, distance, active pace (`min/km`), MET calorie burn, and live route mapping. |

### 📱 Motion & Sensors

| Icon | Tool Name | Sensors / APIs | Description & Key Features |
| :---: | :--- | :--- | :--- |
| 🌐 | **Gravity Meter** | Accelerometer | Total gravitational force $\sqrt{x^2+y^2+z^2}$ in $\text{m/s}^2$ vs. $9.81\,\text{m/s}^2$ with baseline calibration. |
| 🔵 | **Bubble Level** | Accelerometer | 2D spirit bubble level computing pitch and roll angles with bounded circular container graphics. |
| 🌀 | **Shake Detector** | Accelerometer | High-frequency shake detector using low-pass gravity filtering, shake counter, and intensity meter. |
| 🎮 | **Space Ball** | Accelerometer | 2D tilt physics game with velocity vectors, friction damping ($0.93$), and boundary restitution ($0.65$). |

### 🔍 Detection & Measurement

| Icon | Tool Name | Sensors / APIs | Description & Key Features |
| :---: | :--- | :--- | :--- |
| 🧲 | **Metal Detector** | Magnetometer | Detects ferromagnetic distortions relative to Earth's baseline field with proximity meter and geotagged hotspot saving. |
| 📡 | **EMF Meter** | Magnetometer | Measures electromagnetic radiation in microTeslas ($\mu\text{T}$) with safety thresholds and OpenStreetMap spot marking. |

### 🌌 Astronomy & Solar Calculations

| Icon | Tool Name | Sensors / APIs | Description & Key Features |
| :---: | :--- | :--- | :--- |
| 🌙 | **Moon Phase** | Synodic Astronomical Math | Lunar cycle calculator ($29.530588\,\text{days}$) computing current phase emoji, illumination percentage, and next full moon. |
| ☀️ | **Sun Tracker** | Solar Equations + GPS | Computes solar declination ($\delta$), hour angle ($\omega_0$), sunrise, sunset, day length, and live sun azimuth. |

---

## 🧮 Physics, Algorithms & Engineering Principles

1. **Sensor Fusion (Tilt-Compensated Compass)**:
   $$\mathbf{R} = \text{getRotationMatrix}(\mathbf{g}, \mathbf{m}), \quad \theta = \text{getOrientation}(\mathbf{R})$$
   Fuses Accelerometer gravity vector $\mathbf{g}$ and Magnetometer flux vector $\mathbf{m}$ to compute true azimuth without tilt distortion.

2. **Geomagnetic & Gravitational Field Magnitude**:
   $$B_{\text{total}} = \sqrt{B_x^2 + B_y^2 + B_z^2}$$
   Used in the **Gravity Meter**, **Metal Detector**, and **EMF Meter** to measure scalar field density invariant to device orientation.

3. **Cumulative Geodesic Distance (Haversine Formula)**:
   $$d = 2R \arcsin\left(\sqrt{\sin^2\left(\frac{\Delta \phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta \lambda}{2}\right)}\right)$$
   Accumulates precise distance traveled between successive high-accuracy GPS fixes in **Distance Tracker**, **Trail Tracker**, and **Fitness Tracker**.

4. **Metabolic Equivalent of Task (MET) Calorie Calculation**:
   $$\text{Calories Burned} = \text{MET} \times \text{Weight (kg)} \times \text{Duration (hours)}$$
   Estimates active energy expenditure dynamically based on workout speed (walking $\approx 3.5\,\text{MET}$, jogging $\approx 7.0\,\text{MET}$, running $\approx 10.0\,\text{MET}$).

5. **Solar Declination & Hour Angle**:
   $$\delta = 23.44^\circ \sin\left(\frac{360^\circ}{365} (N + 284)\right), \quad \cos(\omega_0) = \frac{\sin(-0.83^\circ) - \sin(\phi)\sin(\delta)}{\cos(\phi)\cos(\delta)}$$
   Accurately predicts sunrise, sunset, and solar position for any geographic coordinate.

---

## 🛠️ Architecture & Tech Stack

* **Language**: Kotlin (100%)
* **Framework**: Android SDK (API 24 to API 34+)
* **Mapping Engine**: OpenStreetMap (`osmdroid-android:6.1.18`)
* **Geocoding & Search**: OpenStreetMap Nominatim REST API
* **Routing & Maneuvers**: Project-OSRM Driving & Turn-by-Turn Routing Engine
* **Location Services**: Google Play Services (`play-services-location:21.0.1`)
* **UI & Components**: Android Material Design 3, ConstraintLayout, RecyclerView, CardView

---

## 📁 Project Structure

```
SensorToolBox/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/sheikhnaim/sensortoolbox/
│   │       │   ├── SensorToolBoxApplication.kt   # Global Context & Theme Initialization
│   │       │   ├── SplashActivity.kt             # Animated Launch Screen
│   │       │   ├── DashboardActivity.kt          # Categorized Tool Hub & Status Monitor
│   │       │   ├── MapActivity.kt                # OpenStreetMap Canvas, Search & Pulse Routing
│   │       │   ├── LiveLocationActivity.kt       # Turn-by-Turn Navigation & GPS Telemetry
│   │       │   ├── FitnessTrackerActivity.kt     # Real-time Pace, Distance & MET Calories
│   │       │   ├── data/
│   │       │   │   ├── ToolItem.kt               # Tool Data Model & Factory Helpers
│   │       │   │   ├── ToolAdapter.kt            # RecyclerView Adapter for Tools
│   │       │   │   └── DashboardDataBuilder.kt   # Complete Tool Catalogue
│   │       │   ├── location/
│   │       │   │   ├── LocationActivity.kt       # Geodetic Coordinates & DMS Inspector
│   │       │   │   ├── TrailTrackerActivity.kt   # GPS Trail Recorder with OpenTopoMap
│   │       │   │   └── TrailView.kt              # Custom Canvas Path Renderer
│   │       │   ├── navigation/
│   │       │   │   └── CompassActivity.kt        # Sensor-Fused Digital Compass
│   │       │   ├── speed/
│   │       │   │   ├── SpeedometerActivity.kt    # Instantaneous Speedometer
│   │       │   │   ├── DistanceTrackerActivity.kt# Trip Odometer & Duration Timer
│   │       │   │   └── AltimeterActivity.kt      # Altitude & Ascent/Descent Gain
│   │       │   ├── motion/
│   │       │   │   ├── GravityMeterActivity.kt   # Vector Gravitational Meter
│   │       │   │   ├── BubbleLevelActivity.kt    # 2D Spirit Bubble Level
│   │       │   │   ├── ShakeDetectorActivity.kt  # Motion Shake Detector
│   │       │   │   └── SpaceBallActivity.kt      # 2D Tilt Physics Engine
│   │       │   ├── detection/
│   │       │   │   ├── MetalDetectorActivity.kt  # Magnetic Anomaly Detector
│   │       │   │   └── EMFMeterActivity.kt       # Electromagnetic Field Gauge
│   │       │   └── astronomy/
│   │       │       ├── MoonPhaseActivity.kt      # Synodic Lunar Phase Calculator
│   │       │       └── SunTrackerActivity.kt     # Solar Position & Sunrise/Sunset Calculator
│   │       ├── res/
│   │       │   ├── layout/                       # Material XML Layouts for All Activities
│   │       │   ├── values/                       # Strings, Colors, Themes & Styles
│   │       │   └── drawable/                     # Icons, Badges & Vector Graphics
│   │       └── AndroidManifest.xml               # App Permissions & Activity Declarations
│   └── build.gradle.kts                          # App Build Configuration & Dependencies
├── build.gradle.kts                              # Root Build Configuration
├── settings.gradle.kts                           # Gradle Project Settings
└── README.md                                     # Project Documentation
```

---

## 🚀 How to Build and Run

1. **Clone the repository**:
   ```bash
   git clone https://github.com/snaimio/AndroidApp3.git
   cd AndroidApp3
   ```

2. **Open in Android Studio**:
   * Open Android Studio -> Select **Open an Existing Project**.
   * Choose the cloned `AndroidApp3` (or `SensorToolBox`) project root directory.

3. **Sync Gradle**:
   * Allow Gradle to download dependencies (`osmdroid`, `play-services-location`, `material`).

4. **Run on Device or Emulator**:
   * Connect an Android device (physical device recommended for full accelerometer and magnetometer sensor testing).
   * Click **Run (▶️)** or execute in terminal:
     ```bash
     ./gradlew assembleDebug
     ```

---

**Developed with ❤️ in Kotlin for Mobile Application Development Assignment 6.**