using UnityEngine;

namespace NWDL.Cast
{
    /// <summary>
    /// Stands in for the headset camera in the Editor: a test pattern, or the PC webcam (e.g. to test QR codes),
    /// seen from the main camera with its field of view.
    /// </summary>
    public sealed class EditorTestFrameSource : MonoBehaviour, IFrameSource
    {
        [Tooltip("Use the PC webcam instead of the test pattern. Falls back to the pattern if there is no webcam.")]
        [SerializeField] private bool useWebcam;

        [SerializeField] private Vector2Int resolution = new(1280, 960);

        private WebCamTexture webcam;

        public Texture Texture { get; private set; }

        public Pose Pose
        {
            get
            {
                var main = Camera.main;
                return main != null ? new Pose(main.transform.position, main.transform.rotation) : Pose.identity;
            }
        }

        public Vector4 NormalizedIntrinsics
        {
            get
            {
                var fov = Camera.main != null ? Camera.main.fieldOfView : 60f;
                var fy = 0.5f / Mathf.Tan(fov * 0.5f * Mathf.Deg2Rad);
                return new Vector4(fy * resolution.y / resolution.x, fy, 0.5f, 0.5f);
            }
        }

        private void OnEnable()
        {
            if (useWebcam && WebCamTexture.devices.Length > 0)
            {
                webcam = new WebCamTexture(WebCamTexture.devices[0].name, resolution.x, resolution.y);
                webcam.Play();
                Texture = webcam;
            }
            else
            {
                Texture = CreateTestPattern(resolution);
            }
        }

        private void OnDisable()
        {
            if (webcam != null) webcam.Stop();
            Destroy(Texture);
            webcam = null;
            Texture = null;
        }

        // Colour bars with a grid every 80 px, so that scaling and alignment errors are easy to see.
        private static Texture2D CreateTestPattern(Vector2Int size)
        {
            Color32[] bars =
            {
                new(235, 235, 235, 255), new(235, 235, 16, 255), new(16, 235, 235, 255), new(16, 235, 16, 255),
                new(235, 16, 235, 255), new(235, 16, 16, 255), new(16, 16, 235, 255), new(16, 16, 16, 255),
            };
            var grid = new Color32(128, 128, 128, 255);
            var pixels = new Color32[size.x * size.y];
            for (var y = 0; y < size.y; y++)
            for (var x = 0; x < size.x; x++)
                pixels[y * size.x + x] = x % 80 == 0 || y % 80 == 0 ? grid : bars[x * bars.Length / size.x];

            var texture = new Texture2D(size.x, size.y, TextureFormat.RGBA32, false) { name = "Cast test pattern" };
            texture.SetPixels32(pixels);
            texture.Apply(false, true);
            return texture;
        }
    }
}
