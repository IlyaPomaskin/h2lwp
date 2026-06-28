#!/usr/bin/env bash
#
# Drive and trace the wallpaper hide/show flow using REAL visibility changes:
#   hide  = open the system Settings app over the wallpaper (-> onVisibilityChanged(false))
#   show  = press Home, back to the launcher                (-> onVisibilityChanged(true))
#
# Timeline uses logcat's own device-side timestamp (applied uniformly to Java and
# native logs by logd), so there's no need to sync clocks between Android and C++.
# Each LWP line is printed as:
#   <abs time>  +<Δ since previous event>  t=<elapsed since first event>  <event>
#
# Usage:
#   scripts/lwp_trace.sh hide     # open Settings (hide)
#   scripts/lwp_trace.sh show     # press Home (show)
#   scripts/lwp_trace.sh toggle   # flip between hide/show (alternates each call)
#   scripts/lwp_trace.sh cycle    # hide, settle, show, settle, then print the timeline
#   scripts/lwp_trace.sh watch    # live timeline — drive it by hand; Ctrl-C to stop
#   scripts/lwp_trace.sh dump     # print the timeline from the current buffer, then exit
#
set -euo pipefail

ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
HIDE_SETTLE="${HIDE_SETTLE:-5}"     # seconds to let the hide fully settle (load+pause)
SHOW_SETTLE="${SHOW_SETTLE:-3}"
STATE_FILE="${TMPDIR:-/tmp}/lwp_trace_state"

do_hide() { "$ADB" shell am start -a android.settings.SETTINGS >/dev/null 2>&1; }   # cover the wallpaper
do_show() { "$ADB" shell input keyevent KEYCODE_HOME >/dev/null 2>&1; }              # back to launcher

# Parse `adb logcat -v threadtime` on stdin -> timed LWP timeline.
parse() {
    awk '
    /LWP/ {
        # threadtime format: MM-DD HH:MM:SS.mmm PID TID LEVEL TAG: message
        split($2, t, /[:.]/)
        ms = ((t[1] * 3600) + (t[2] * 60) + t[3]) * 1000 + t[4]
        if (first == "") first = ms
        d = (prev == "") ? 0 : ms - prev
        prev = ms

        ev = substr($0, index($0, "LWP"))   # text from the first "LWP"
        sub(/^LWP +: +/, "", ev)            # drop the Java "LWP:" tag form
        sub(/^LWP /, "", ev)                # drop the native "LWP " prefix

        printf "%s  +%6d ms   t=%7d ms   %s\n", $2, d, ms - first, ev
    }'
}

"$ADB" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1

case "${1:-cycle}" in
    hide)
        do_hide; echo hidden > "$STATE_FILE"
        ;;
    show)
        do_show; echo visible > "$STATE_FILE"
        ;;
    toggle)
        if [ "$(cat "$STATE_FILE" 2>/dev/null || echo visible)" = visible ]; then
            do_hide; echo hidden > "$STATE_FILE"; echo "-> hide (Settings)"
        else
            do_show; echo visible > "$STATE_FILE"; echo "-> show (Home)"
        fi
        ;;
    cycle)
        do_show; sleep "$SHOW_SETTLE"          # start from a known visible state
        "$ADB" logcat -c
        do_hide; sleep "$HIDE_SETTLE"          # hide -> load map -> render -> pause
        do_show; sleep "$SHOW_SETTLE"          # show -> resume
        echo visible > "$STATE_FILE"
        "$ADB" logcat -d -v threadtime | parse
        ;;
    watch)
        "$ADB" logcat -c
        echo "Watching LWP timeline — open an app / press Home on the device (Ctrl-C to stop)..."
        "$ADB" logcat -v threadtime | parse
        ;;
    dump)
        "$ADB" logcat -d -v threadtime | parse
        ;;
    *)
        echo "usage: $0 {hide|show|toggle|cycle|watch|dump}" >&2
        exit 1
        ;;
esac
