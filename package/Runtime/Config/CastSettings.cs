using UnityEngine;

namespace NWDL.Cast
{
    public enum StreamCodec { VP8, H264 }

    /// <summary>Stream settings, shared by all headsets of an app. Device credentials are not stored here (see <see cref="DeviceCredentials"/>).</summary>
    [CreateAssetMenu(menuName = "NWDL Cast/Cast Settings", fileName = "CastSettings")]
    public sealed class CastSettings : ScriptableObject
    {
        [Tooltip("Token API of the cast website.")]
        public string tokenEndpoint = "https://cast.nwdl.org/api/token/device";

        [Tooltip("Resolution of the top stream layer. The Quest camera delivers 1280x960.")]
        public Vector2Int streamResolution = new(1280, 960);

        [Tooltip("Frame rate of the top stream layer. The default is chosen after the 30 vs. 60 fps test on the Quest.")]
        [Range(1, 60)]
        public int fps = 30;

        [Tooltip("Bitrate of the top stream layer in bit/s.")]
        public int maxBitrate = 3_000_000;

        public StreamCodec codec = StreamCodec.VP8;

        [Tooltip("With passthrough: layers drawn as virtual content over the camera image. Nothing = camera image only.")]
        public LayerMask virtualContentLayers = ~0;
    }
}
