# AI Reflection - Assignment 5

## 1. How Did You Use AI in This Assignment?

### a. Summarize how you used AI-generated code overall.

I used AI a lot. I kept about 40% of the AI code as-is, modified about 40% to make it work, and used 20% just to understand what to do.

### b. Include one or two examples that show your process.

**Example 1:** I asked AI: "How does the compass code in the LocationFinder tutorial work?" AI explained sensor fusion (accelerometer + magnetometer). I used the professor's code but added a rotating compass image and status text myself.

**Example 2:** I asked AI: "How do I make a metal detector?" AI gave me code for `Sensor.TYPE_MAGNETIC_FIELD`. I kept the sensor code but added a progress bar and color changes myself.

**What I didn't use:** AI suggested `TYPE_ORIENTATION` but I used `getRotationMatrix()` from the professor's tutorial instead.

### c. If you encountered any concepts not yet covered in class, briefly explain how you researched or learned about them.

**Sensor Fusion:** I watched YouTube videos and re-read the LocationFinder tutorial to understand `getRotationMatrix()`.

**Geocoding:** The professor's tutorial showed it, but I researched the modern API for Android 13+.

**Magnetic Field:** I read Android documentation to understand how the magnetometer detects metals.

---

## 2. How Did You Understand, Verify, and Adapt the Code?

### a. Explain how you verified the correctness of any AI-generated code.

| Method | What I Did |
|--------|------------|
| Testing on my phone | Tested every tool on my physical device |
| Logcat | Read error messages when the app crashed |
| Log.d() | Printed sensor values to see what was happening |
| Trial and error | Changed things until they worked |

**Example:** For the compass, I added `Log.d()` to see the bearing value. I noticed it was in radians, so I converted it to degrees.

### b. Describe one or two key changes or improvements you made.

**Change 1 - Compass Visibility:**
```
Problem: compass.png was invisible on dark background
Fix: Added CardView with light background (#F5F5F5) behind it
Reason: Image needed contrast to be visible
```

**Change 2 - Consistent Buttons:**
```
Problem: Buttons were different sizes
Fix: Used fixed height (48dp) and minWidth (100dp) for all
Reason: App looked messy with inconsistent buttons
```

---

## 3. What Did You Learn or Get Better At Through This Work?

### a. Reflect on at least one concept, practice, or skill where you feel you levelled up.

**Sensor Lifecycle:** I finally understand why `onResume()` and `onPause()` are needed. Before, I just copied the code. Now I know it's to save battery by stopping sensors when the app is in the background.

### b. Briefly describe what went well and what didn't.

**What went well:**
- All 14 tools work
- Compass sensor fusion works
- GPS shows correct coordinates

**What didn't go well:**
- Wasted time trying to use the emulator (no sensors)
- Space Ball physics took a lot of tweaking
- Kept getting confused with Git and Android Studio freezing

**What I learned:** Test sensors on a physical device from the start. Use `Log.d()` to debug sensor values.