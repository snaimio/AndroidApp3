# AI Reflection - Assignment 5

## 1. How Did You Use AI in This Assignment?

### a. Overall Summary

I used AI a lot for this assignment. Honestly, I don't think I could have finished it without it. I kept asking AI for help whenever I got stuck.

I used AI for:
- Generating code when I didn't know where to start
- Explaining why things weren't working
- Helping me understand the professor's code
- Fixing errors

### b. Examples of My Process

**Example 1 - Space Ball:**
I wanted to make a ball move when you tilt the phone. I had no idea how to do this. I asked AI and got some code. I copied it into my project but it didn't work. The ball wouldn't move. I kept asking AI why and eventually figured out I needed to adjust the sensitivity values. I also had to add bounce physics myself.

**Example 2 - Metal Detector:**
I asked AI how to make a metal detector. It gave me code that reads the magnetic sensor. I copied it and it worked. I added a progress bar and color changes to make it look better.

**What I didn't use:** AI suggested using a library for physics, but I just used basic math because I didn't want to add extra dependencies.

### c. New Concepts I Had to Learn

I had to learn several new things. I didn't know what sensor fusion was before this assignment. I had to look up how `getRotationMatrix()` works. I also had to learn how to use Git because I had never used it before.

---

## 2. How Did You Understand, Verify, and Adapt the Code?

### a. How I Verified My Code

I tested everything on my phone. The emulator didn't work for sensors. I also used Logcat a lot to see what was happening when my app crashed.

**What I did to test:**
- Walked around outside to test GPS
- Rotated my phone to test the compass
- Moved my phone near metal to test the metal detector
- Shook my phone to test the shake detector

### b. Changes I Made

**Change 1 - Layout Fixes:**
My buttons were all different sizes. I changed them to have the same height and width. It looked much better after.

**Change 2 - Compass Image:**
I used the professor's code for the compass but added a rotating image because I wanted it to look nicer.

**Change 3 - Dark Theme:**
I changed the theme to dark because I preferred it. This required changing multiple layout files.

---

## 3. What Did You Learn or Get Better At Through This Work?

### a. What I Learned

I learned that I should ask better questions to AI. Sometimes I just copied code without understanding it, and then it didn't work. When I asked AI to explain concepts instead of just giving me code, I understood things better.

**Sensor Lifecycle:** I learned why we need `onResume()` and `onPause()` for sensors. The professor mentioned it, but I didn't really understand until I had to do it myself.

### b. What Went Well and What Didn't

**What went well:**
- Most of the tools work
- The compass shows the correct direction
- GPS shows my location

**What didn't go well:**
- I wasted time trying to get the emulator to work
- I kept getting confused with Git
- Some tools took too much time (Space Ball)
- I didn't understand sensor fusion at first

**What I learned:** Ask AI for explanations, not just code. Test on a real phone early. Git commands get easier with practice.