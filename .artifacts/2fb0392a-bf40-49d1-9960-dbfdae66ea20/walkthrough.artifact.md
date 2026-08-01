# Walkthrough - UI/UX Overhaul (RouteGo)

I have successfully revamped the UI/UX of the application to make it look professional and unique. The app has been renamed to **RouteGo** and features a modern, map-centric layout.

## Changes Made

### Branding & Visuals
- **App Name**: Updated to "RouteGo".
- **Theme**: Switched to a custom **Deep Blue & Indigo** palette.
- **Dynamic Color**: Disabled by default to ensure the custom branding is consistent across all devices.
- **Icons**: Used modern `Outlined` icons for a cleaner aesthetic.

### Layout & Interaction
- **Floating Search Card**: The origin and destination inputs are now housed in a floating elevated card at the top, mimicking premium map applications.
- **Refined Bottom Sheet**: The bottom sheet now focuses on displaying results and radius controls, with a softer corner radius (24dp).
- **Transport Selection**: Redesigned transport mode selection using compact chips.
- **Status Badge**: Added a stylized "Dalam Target" / "Diluar Target" badge for better visual feedback.

### Map Customization
- **Map Styles**: Refined the dark mode map style to match the new Deep Blue theme.
- **Visual Aids**: Updated the target radius circle with semi-transparent fills that change color based on the status.

## Verification Results

### Build Status
- [x] Gradle Build: `app:assembleDebug` passed successfully.

### UI Preview
![RouteGo UI Preview](file:///D:/MyNote/UAS_PEMROGRAMAN-MOBILE_MyMap/.artifacts/2fb0392a-bf40-49d1-9960-dbfdae66ea20/scratch/preview.png)

> [!NOTE]
> The preview shows the new floating search card, transport chips, and the simplified bottom sheet.

### Tested Components
- Dark Mode Toggling
- Search Field Input & Clearing
- Mode Selection
- Radius Slider & Toggle
