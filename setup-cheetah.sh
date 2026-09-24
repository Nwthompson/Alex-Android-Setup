#!/bin/bash
# Sync Android 15 for Pixel 7 Pro (cheetah) and apply the wake-to-home overlay.
set -euo pipefail

root=$(cd "$(dirname "$0")" && pwd)
dest=${1:-"$HOME/aosp-cheetah"}
tag=android-15.0.0_r34
blob_url=https://dl.google.com/dl/android/aosp/google_devices-cheetah-bp1a.250505.005.b1-e217d0cd.tgz
blob_name=google_devices-cheetah-bp1a.250505.005.b1-e217d0cd.tgz

if [[ -e "$dest" && ! -d "$dest/.repo" ]]; then
  echo "$dest exists and is not an AOSP tree" >&2
  exit 1
fi

mkdir -p "$dest"
cd "$dest"

if [[ ! -d .repo ]]; then
  repo init --partial-clone --clone-filter=blob:limit=10M \
    -u https://android.googlesource.com/platform/manifest \
    -b "$tag"
fi

repo sync -c -j8

if [[ ! -d vendor/google_devices/cheetah ]]; then
  tmp=$(mktemp -d)
  curl -fL "$blob_url" -o "$tmp/$blob_name"
  tar -C "$tmp" -xzf "$tmp/$blob_name"
  extract=$(echo "$tmp"/extract-google_devices-cheetah.sh)
  # The extract script prompts for the license. Accept it non-interactively.
  (cd "$dest" && printf 'I ACCEPT\n' | bash "$extract")
  rm -rf "$tmp"
fi

cp "$root/device/google/pantah/aosp_cheetah.mk" device/google/pantah/aosp_cheetah.mk

python3 - "$dest" << 'PY'
import pathlib, sys
root = pathlib.Path(sys.argv[1])

lock = root / "frameworks/base/core/java/com/android/internal/widget/LockPatternUtils.java"
text = lock.read_text()
needle = "boolean disabledByDefault = mContext.getResources().getBoolean("
insert = """boolean disabledByProperty = android.os.SystemProperties.getBoolean(
                "ro.lockscreen.disable.default", false);
        """
if "ro.lockscreen.disable.default" not in text:
    if needle not in text:
        raise SystemExit("LockPatternUtils.java: expected disabledByDefault")
    text = text.replace(needle, insert + needle, 1)
    old = "return getBoolean(DISABLE_LOCKSCREEN_KEY, false, userId) || disabledByDefault || isDemoUser;"
    new = "return getBoolean(DISABLE_LOCKSCREEN_KEY, false, userId)\n                || disabledByDefault || isDemoUser || disabledByProperty;"
    if old not in text:
        raise SystemExit("LockPatternUtils.java: expected return expression")
    text = text.replace(old, new, 1)
    lock.write_text(text)

pwm = root / "frameworks/base/services/core/java/com/android/server/policy/PhoneWindowManager.java"
ptext = pwm.read_text()
marker = 'mKeyguardDelegate.onStartedWakingUp(pmWakeReason, mPowerButtonLaunchGestureTriggered);'
block = """
        mPowerButtonLaunchGestureTriggered = false;

        if (android.os.SystemProperties.getBoolean("ro.vendor.display.wake_to_home", false)
                && displayGroupId == Display.DEFAULT_DISPLAY_GROUP
                && mLockPatternUtils.isLockScreenDisabled(mCurrentUserId)) {
            startDockOrHome(DEFAULT_DISPLAY, false /* fromHomeKey */, true /* awakenFromDreams */,
                    "display power on");
        }
"""
if "ro.vendor.display.wake_to_home" not in ptext:
    if marker not in ptext:
        raise SystemExit("PhoneWindowManager.java: expected onStartedWakingUp")
    ptext = ptext.replace(marker, marker + block, 1)
    pwm.write_text(ptext)
print("overlay applied")
PY

echo "Tree ready at $dest"
echo "Next, from that tree: source build/envsetup.sh && lunch aosp_cheetah-bp1a-userdebug && m"
