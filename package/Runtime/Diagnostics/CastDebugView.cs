using UnityEngine;

namespace NWDL.Cast
{
    /// <summary>
    /// Shows the frame source on a renderer, e.g. a quad in front of the camera, to check the image in the
    /// Editor and on the headset. Uses the IFrameSource on this GameObject or adds the one for this platform.
    /// </summary>
    public sealed class CastDebugView : MonoBehaviour
    {
        [SerializeField] private Renderer target;

        private IFrameSource source;

        private void Start()
        {
            source = TryGetComponent(out IFrameSource existing) ? existing : FrameSource.AddTo(gameObject);
        }

        private void Update()
        {
            var texture = source?.Texture;
            if (target != null && target.material.mainTexture != texture) target.material.mainTexture = texture;
        }
    }
}
