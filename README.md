# 🧰 Sensor ToolBox

## 📱 Assignment 5 - Android Development

A comprehensive Android app featuring **14 sensor-based tools** using GPS, Accelerometer, and Magnetic Field sensors.

---

## 📊 Features

### 🧭 Location & Navigation

| Tool | Sensors | Description |
|------|---------|-------------|
| **Digital Compass** | Accelerometer + Magnetometer | Shows heading (0-360°) and cardinal direction |
| **GPS Location** | GPS | Displays latitude, longitude, altitude, and address |
| **Trail Tracker** | GPS | Records hiking trails with distance, time, and elevation |

### 📊 Speed & Distance

| Tool | Sensors | Description |
|------|---------|-------------|
| **Speedometer** | GPS | Shows current, max, and average speed |
| **Distance Tracker** | GPS | Tracks total distance with elapsed time |
| **Altimeter** | GPS | Shows altitude, elevation gain/loss |

### 📱 Motion & Sensors

| Tool | Sensors | Description |
|------|---------|-------------|
| **Gravity Meter** | Accelerometer | Measures gravitational force (X/Y/Z) |
| **Bubble Level** | Accelerometer | Pitch/roll angles with moving bubble |
| **Shake Detector** | Accelerometer | Counts shakes with intensity |
| **Space Ball** | Accelerometer | Physics game controlled by tilting |

### 🔍 Detection

| Tool | Sensors | Description |
|------|---------|-------------|
| **Metal Detector** | Magnetometer | Detects metals by magnetic field |
| **EMF Meter** | Magnetometer | Measures electromagnetic field strength |

### 🌌 Astronomy

| Tool | Sensors | Description |
|------|---------|-------------|
| **Moon Phase** | GPS + Math | Shows current moon phase and illumination |
| **Sun Tracker** | GPS + Math | Displays sunrise, sunset, and sun position |

---

## 🛠️ Technologies Used

| Technology | Purpose |
|------------|---------|
| **Kotlin** | Main programming language |
| **Android SDK** | Android development framework |
| **Material Design** | UI components and styling |
| **Google Play Services** | GPS and location services |
| **SensorManager** | Access to device sensors |
| **RecyclerView** | Efficient list display |

---

## 🎯 LocationFinder Features Applied

| Feature | Implementation |
|---------|----------------|
| ✅ Permission Handling | Runtime location permissions |
| ✅ FusedLocationProviderClient | High-accuracy GPS |
| ✅ SensorManager | Accelerometer, Magnetometer |
| ✅ Sensor Fusion | Compass (Accel + Magnetic) |
| ✅ Geocoding | Reverse geocoding for addresses |
| ✅ Lifecycle Management | onResume/onPause for sensors |
| ✅ Error Handling | Toast messages for failures |
| ✅ Toolbar | Navigation with back button |
| ✅ Menu Navigation | Dashboard with tool grid |
| ✅ UI Updates | Real-time sensor data display |

---

### Dashboard
```
┌─────────────────────────────────────────────┐
│ 🧰 Sensor ToolBox                           │
├─────────────────────────────────────────────┤
│                                             │
│  📍 LOCATION & NAVIGATION                  │
│  ┌────────┐ ┌────────┐ ┌────────┐        │
│  │ 🧭     │ │ 📍     │ │ 🥾     │        │
│  │Compass │ │GPS     │ │Trail  │        │
│  │[OPEN]  │ │[OPEN]  │ │[OPEN] │        │
│  └────────┘ └────────┘ └────────┘        │
│                                             │
│  📊 SPEED & DISTANCE                       │
│  ┌────────┐ ┌────────┐ ┌────────┐        │
│  │ 🏎️     │ │ 📏     │ │ ⛰️     │        │
│  │Speedo  │ │Dist    │ │Altim  │        │
│  └────────┘ └────────┘ └────────┘        │
│                                             │
│  ... more tools ...                        │
│                                             │
│  📡 GPS: ✅ Locked                        │
└─────────────────────────────────────────────┘
```

### Digital Compass
```
┌─────────────────────────────────────────┐
│ ←  🧭 Digital Compass                  │
├─────────────────────────────────────────┤
│                                         │
│        [COMPASS IMAGE]                  │
│                                         │
│             43°                         │
│                                         │
│          Northeast                      │
│                                         │
│   📡 Bearing: 43° Northeast            │
└─────────────────────────────────────────┘
```

---

## 📁 Project Structure

```
SensorToolBox/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/sheikhnaim/sensortoolbox/
│   │       │   ├── DashboardActivity.kt
│   │       │   ├── SplashActivity.kt
│   │       │   ├── SensorToolBoxApplication.kt
│   │       │   ├── data/
│   │       │   │   ├── ToolItem.kt
│   │       │   │   └── ToolAdapter.kt
│   │       │   ├── navigation/
│   │       │   │   └── CompassActivity.kt
│   │       │   ├── location/
│   │       │   │   ├── LocationActivity.kt
│   │       │   │   └── TrailTrackerActivity.kt
│   │       │   ├── speed/
│   │       │   │   ├── SpeedometerActivity.kt
│   │       │   │   ├── DistanceTrackerActivity.kt
│   │       │   │   └── AltimeterActivity.kt
│   │       │   ├── motion/
│   │       │   │   ├── GravityMeterActivity.kt
│   │       │   │   ├── BubbleLevelActivity.kt
│   │       │   │   ├── ShakeDetectorActivity.kt
│   │       │   │   └── SpaceBallActivity.kt
│   │       │   ├── detection/
│   │       │   │   ├── MetalDetectorActivity.kt
│   │       │   │   └── EMFMeterActivity.kt
│   │       │   └── astronomy/
│   │       │       ├── MoonPhaseActivity.kt
│   │       │       └── SunTrackerActivity.kt
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   ├── drawable/
│   │       │   ├── values/
│   │       │   └── anim/
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── README.md
├── AIReflection.md
├── LICENSE
└── .gitignore
```

---

## 🚀 How to Run

1. **Clone the repository**
   ```bash
   git clone https://github.com/snaimio/AndroidApp3.git
   ```

2. **Open in Android Studio**
    - Open Android Studio
    - Select "Open an existing project"
    - Navigate to the project folder

3. **Sync Gradle**
    - Click "Sync Now" when prompted

4. **Run the app**
    - Connect an Android device (physical device recommended for sensors)
    - Click the Run button (▶️)

---

**Built with ❤️ using Kotlin and Android Studio**