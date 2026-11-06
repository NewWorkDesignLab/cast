# NWDL Cast (Unity package)

Streams the passthrough camera image of a Meta Quest 3 / 3S, combined with the app's virtual content, to `https://cast.nwdl.org`. Streaming is started and stopped only on the headset, from app code or by QR code. The package ships no UI.

Status: early development. Capture, compositing and streaming follow.

## Requirements

- Unity 6000.3 with URP
- Meta XR SDK with MRUK 81 or later (e.g. `com.meta.xr.sdk.all`)
- Git LFS (the LiveKit SDK stores its native libraries in LFS)

## Installation

Add both packages to `Packages/manifest.json` of your project. The LiveKit SDK is a Git package, so UPM cannot pull it in as a dependency:

```json
{
  "dependencies": {
    "io.livekit.livekit-sdk": "https://github.com/livekit/client-sdk-unity.git#v2.1.1",
    "org.nwdl.cast": "https://github.com/NewWorkDesignLab/cast.git?path=/package"
  }
}
```

For development against a local checkout, use `"org.nwdl.cast": "file:<path to the checkout>/package"` instead.

## Device credentials

Each headset needs its own device ID and secret. They are never built into the app.

1. Create them in the `web/` folder of the cast repository: `npm run new-device -- <deviceId>`. The output contains the `cast.json` for the headset and the entry for the `DEVICES` variable of the website.
2. Copy `cast.json` to the app's persistent data folder on the headset:

   ```sh
   adb push cast.json /sdcard/Android/data/<your.bundle.id>/files/cast.json
   ```

Without the file the app logs a warning and keeps running without streaming.

## Debug view

Add `CastDebugView` to a GameObject and assign a renderer, e.g. a quad with an unlit material. In the Editor it shows a test pattern (or the PC webcam, see `EditorTestFrameSource`).
