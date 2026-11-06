# NWDL Cast (headset app)

Android app for Meta Quest 3 / 3S. It casts what the wearer sees, with microphone and app sound, to `https://cast.nwdl.org`. The wearer starts and stops casting in the app; the system asks for consent each time. Other apps need no integration.

## Build

Needs JDK 17 and an Android SDK with platform 36 and build-tools 36.0.0. Unity's Android module ships both:

```sh
# local.properties (not committed)
sdk.dir=C:/Program Files/Unity/Hub/Editor/6000.3.10f1/Editor/Data/PlaybackEngines/AndroidPlayer/SDK
```

```sh
JAVA_HOME="C:/Program Files/Unity/Hub/Editor/6000.3.10f1/Editor/Data/PlaybackEngines/AndroidPlayer/OpenJDK" ./gradlew assembleDebug
adb install -r build/outputs/apk/debug/cast-debug.apk
```

The app shows up under "Unknown Sources" in the headset library.

## Device credentials

Each headset needs its own device ID and secret, never built into the app.

1. In `web/`: `npm run new-device -- <deviceId>`. Add the printed entry to `DEVICES` of the website.
2. Save the printed `cast.json` and copy it to the headset:

   ```sh
   adb shell mkdir -p /sdcard/Android/data/org.nwdl.cast/files
   adb push cast.json /sdcard/Android/data/org.nwdl.cast/files/cast.json
   ```

`cast.json` may contain `tokenEndpoint` to use another server than `https://cast.nwdl.org/api/token/device`.

## Local testing over USB

Start the local stack (`server/livekit-dev.ps1`, `npm run dev` in `web/`), then forward its ports to the headset:

```sh
adb reverse tcp:4321 tcp:4321   # website / token API
adb reverse tcp:7880 tcp:7880   # LiveKit signalling
adb reverse tcp:7881 tcp:7881   # LiveKit media over TCP
```

and use `"tokenEndpoint": "http://localhost:4321/api/token/device"` in `cast.json`. Debug builds allow plain http/ws for this.
