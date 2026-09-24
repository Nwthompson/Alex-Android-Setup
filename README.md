# Android 15 for Pixel 7 Pro

This tree builds `aosp_cheetah` from AOSP tag `android-15.0.0_r34` (factory build `BP1A.250505.005.B1`) and opens the launcher on display wake when no PIN, pattern, or password is set.

The plugged-in Pixel 7 Pro (`27231FDH3007FS`) is Android 16 build `CP1A.260405.005`, security patch 2026-04-05, bootloader locked, verified boot green. Do not flash this Android 15 image onto it. The bootloader must be unlocked first, which wipes the phone, and the Android 16 rollback index is newer than this Android 15 build. Flashing an older bootloader can permanently brick the device.

## Setup

```bash
./setup-cheetah.sh /path/to/aosp-cheetah
```

That syncs `android-15.0.0_r34`, downloads the matching Pixel 7 Pro vendor binaries, and copies [device/google/pantah/aosp_cheetah.mk](device/google/pantah/aosp_cheetah.mk) into the tree. `ro.vendor.display.wake_to_home` is a vendor property. `ro.lockscreen.disable.default` is a product property because `vendor_init` cannot set it.

## Build

From the AOSP tree:

```bash
source build/envsetup.sh
lunch aosp_cheetah-bp1a-userdebug
m
```

A credential still keeps the lock screen. `isLockScreenDisabled()` stays false when a PIN, pattern, or password is set, and `startedWakingUp()` only calls `startDockOrHome()` when the lock screen is disabled.
