using Meta.XR;
using UnityEngine;
#if UNITY_ANDROID && !UNITY_EDITOR
using UnityEngine.Android;
#endif

namespace NWDL.Cast
{
    /// <summary>Headset camera image via MRUK's PassthroughCameraAccess. Added and removed by the usePassthrough toggle of <see cref="CastStreamer"/>.</summary>
    [RequireComponent(typeof(PassthroughCameraAccess))]
    public sealed class PassthroughFrameSource : MonoBehaviour, IFrameSource
    {
        private const string CameraPermission = "horizonos.permission.HEADSET_CAMERA";

        private PassthroughCameraAccess cameraAccess;

        public bool IsAvailable => cameraAccess.enabled;

        public Texture Texture => cameraAccess.enabled && cameraAccess.IsPlaying ? cameraAccess.GetTexture() : null;

        public Pose Pose => cameraAccess.GetCameraPose();

        public Vector4 NormalizedIntrinsics
        {
            get
            {
                // The image is a centred crop of the sensor with the aspect ratio of the current resolution, as in PassthroughCameraAccess.
                var intrinsics = cameraAccess.Intrinsics;
                var sensor = (Vector2)intrinsics.SensorResolution;
                var scale = (Vector2)cameraAccess.CurrentResolution / sensor;
                scale /= Mathf.Max(scale.x, scale.y);
                var crop = sensor * scale;
                var offset = (sensor - crop) * 0.5f;
                return new Vector4(
                    intrinsics.FocalLength.x / crop.x,
                    intrinsics.FocalLength.y / crop.y,
                    (intrinsics.PrincipalPoint.x - offset.x) / crop.x,
                    (intrinsics.PrincipalPoint.y - offset.y) / crop.y);
            }
        }

        // Off until Awake decides, so that the camera does not start where there is none.
        private void Reset() => GetComponent<PassthroughCameraAccess>().enabled = false;

        private void Awake()
        {
            cameraAccess = GetComponent<PassthroughCameraAccess>();
#if UNITY_EDITOR
            // Real camera over Meta Horizon Link; without a headset CastStreamer uses the test source.
            var headset = OVRPlugin.GetSystemHeadsetType();
            cameraAccess.enabled = headset is OVRPlugin.SystemHeadset.Meta_Link_Quest_3 or OVRPlugin.SystemHeadset.Meta_Link_Quest_3S;
#elif UNITY_ANDROID
            if (!Permission.HasUserAuthorizedPermission(CameraPermission)) Permission.RequestUserPermission(CameraPermission);
            cameraAccess.enabled = true; // waits until the permission is granted
#endif
        }
    }
}
