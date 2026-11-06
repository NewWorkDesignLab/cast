# NWDL Cast (headset app)

Android app for Meta Quest 3 / 3S. It casts what the wearer sees, with microphone and app sound, to `https://cast.nwdl.org`. The wearer starts and stops casting in the app; viewers can end a cast on the website. Other apps need no integration.

## Pairing

A new headset shows a pairing notice instead of the casting buttons and looks for a QR code with the passthrough camera.

1. On a computer, open `cast.nwdl.org/headsets`, sign in and choose "Add a headset".
2. Look at the QR code with the headset. It works once and expires after 10 minutes.

The headset then has permanent credentials, stored privately in the app. Removing it on `/headsets` revokes them; the app asks to pair again. Release builds only pair with `https://cast.nwdl.org`.

Android asks for consent before each cast (MediaProjection). On headsets in developer mode this can be skipped once per headset: `adb shell appops set org.nwdl.cast PROJECT_MEDIA allow`.

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

## Local testing over USB

Start the local stack (`server/livekit-dev.ps1`, `npm run dev` in `web/`), then forward the website and LiveKit signalling to the headset. Media goes directly over the network, so the headset must be in the same network as the PC.

```sh
adb reverse tcp:4321 tcp:4321   # website / API
adb reverse tcp:7880 tcp:7880   # LiveKit signalling
```

Pair with the QR code from `http://localhost:4321/headsets` (debug builds accept any server and plain http/ws). The forwarding is lost when the headset reconnects; run the commands again.

Debug builds also have a button that tries to skip the consent dialog through the headset's own adb daemon (needs `adb tcpip 5555` once; the headset asks to allow the app's adb key).
