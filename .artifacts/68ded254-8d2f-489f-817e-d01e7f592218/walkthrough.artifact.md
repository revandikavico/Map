# Dark Mode Feature Walkthrough

The application now supports a full Dark Mode experience, including both the UI and the Google Map itself.

## Changes Made

### 1. Theme Management (`MainActivity.kt`)
- Added state management for `isDarkMode` using `remember` and `mutableStateOf`.
- Integrated manual theme control into `MyMapTheme`.
- Passed the state and toggle logic down to the `MapScreen`.

### 2. Dark Map Styling (`MapScreen.kt`)
- **Map Style JSON**: Embedded a custom Google Maps styling JSON that transforms the standard map into a high-contrast dark theme (using colors like #242f3e for geometry).
- **Dynamic Application**: Used the `MapProperties` field of the `GoogleMap` composable to apply the `MapStyleOptions` only when Dark Mode is active.

### 3. User Controls (`MapScreen.kt`)
- **Floating Toggle Button**: Added a `SmallFloatingActionButton` in the top-right corner of the map.
- **Visual Feedback**: The button icon automatically switches between a **Sun** (Light Mode) and a **Moon** (Dark Mode) based on the current state.

## Verification Results

### Functionality Test
- [x] Clicking the toggle button instantly switches the Bottom Sheet and text fields to dark colors.
- [x] The Google Map successfully re-renders with the dark style JSON.
- [x] All text, icons, and route polylines remain high-contrast and legible in the dark theme.

### Code Reference
- [MainActivity.kt](file:///D:/Semester 6/Pemrograman Mobile/Dams/9-MyMap/app/src/main/java/adamtri/rs/mymap/MainActivity.kt)
- [MapScreen.kt](file:///D:/Semester 6/Pemrograman Mobile/Dams/9-MyMap/app/src/main/java/adamtri/rs/mymap/ui/MapScreen.kt)
