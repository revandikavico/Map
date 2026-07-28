# Implementation Plan - Dark Mode Feature

Add a dark mode toggle to the application that switches both the UI theme and the Google Map style.

## Proposed Changes

### UI & Theme Layer

#### [MODIFY] [MainActivity.kt](file:///D:/Semester 6/Pemrograman Mobile/Dams/9-MyMap/app/src/main/java/adamtri/rs/mymap/MainActivity.kt)
- Create a `mutableStateOf` for `isDarkMode`.
- Pass this state into the `MyMapTheme` call.
- Pass the state and a toggle function into `MapScreen`.

#### [MODIFY] [MapScreen.kt](file:///D:/Semester 6/Pemrograman Mobile/Dams/9-MyMap/app/src/main/java/adamtri/rs/mymap/ui/MapScreen.kt)
- **New State**: Receive `isDarkMode` and `onToggleDarkMode` as parameters.
- **Floating Toggle**: Add a floating `IconButton` with a sun/moon icon on the map to toggle dark mode.
- **Map Styling**:
    - Define a `mapStyleJson` string for Dark Mode.
    - Update `MapProperties` in the `GoogleMap` composable to use `MapStyleOptions(mapStyleJson)` when `isDarkMode` is true.

## Verification Plan

### Manual Verification
1. **Toggle UI**: Click the floating dark mode button. Verify that the bottom sheet, text fields, and icons switch to a dark theme.
2. **Toggle Map**: Verify that the Google Map itself switches to a dark, high-contrast style.
3. **Icons/Text Legibility**: Ensure all text and icons remain clearly visible in both light and dark modes.
