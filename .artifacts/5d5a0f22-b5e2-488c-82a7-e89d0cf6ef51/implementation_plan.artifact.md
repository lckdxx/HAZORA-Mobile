# Fix Manifest Merger and Dependency Issues

The project is failing to build due to a `Manifest merger failed` error. This is often caused by incompatible library versions, missing `android:exported` attributes (though verified in the main manifest), or using experimental SDK/AGP versions that aren't fully supported by the environment.

Additionally, there are clear errors in `libs.versions.toml` (specifically `activityKtx` version) and inconsistencies in `app/build.gradle.kts`.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/Lenovo/OneDrive/Documents/GitHub/HAZORA-Mobile/gradle/libs.versions.toml)
- Update `agp` to `8.7.0` (stable) to ensure better compatibility with current tools.
- Update `compileSdk` and `targetSdk` to `35` (Android 15), as `36` is not yet stable/widely available.
- Fix `activityKtx` version to `1.9.3` to match `androidxActivity`.
- Update `androidxCore` to `1.13.1` (matching `app/build.gradle.kts`) or standardize on `1.15.0`.

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/Lenovo/OneDrive/Documents/GitHub/HAZORA-Mobile/app/build.gradle.kts)
- Standardize dependencies to use `libs` instead of hardcoded versions.
- Update `compileSdk` and `targetSdk` to `35`.
- Remove the non-standard `optimization` block in `buildTypes.release`.
- Apply the `kotlin-android` plugin if Kotlin is being used (although I only see Java files, some dependencies are Kotlin-based).

### Manifest

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/Lenovo/OneDrive/Documents/GitHub/HAZORA-Mobile/app/src/main/AndroidManifest.xml)
- Add `android:required="false"` to the `uses-feature` for `android.hardware.camera.any` to avoid potential merger conflicts with libraries that might define it differently.

## Verification Plan

### Automated Tests
- Run `./gradlew clean :app:processDebugMainManifest` to verify the manifest merges successfully.
- Run `./gradlew assembleDebug` to ensure the project builds.

### Manual Verification
- Verify the build finishes without "Manifest merger failed" in the Android Studio Build tab.
