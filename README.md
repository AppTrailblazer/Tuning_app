# PianoMeter

## Developer Notes

### Native Code Debugging

To be able to debug native code, make sure you have installed CMake and LLDB (Android SDK Tools).

Then, edit the run configuration in Android Studio via `app -> Edit Configurations -> Debugger`, and set `Debug Type = Dual`.

### Dependencies management

Dependencies are managed using Version Catalogs.

To fetch and update dependencies versions, use `./gradlew versionCatalogUpdate`