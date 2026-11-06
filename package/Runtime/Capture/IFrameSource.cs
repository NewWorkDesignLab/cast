using UnityEngine;

namespace NWDL.Cast
{
    /// <summary>Delivers the camera image the stream is built from.</summary>
    public interface IFrameSource
    {
        /// <summary>Latest camera image, null until the source is ready.</summary>
        Texture Texture { get; }
    }

    public static class FrameSource
    {
        /// <summary>Adds the frame source for this platform: a test source in the Editor, the passthrough camera on the headset.</summary>
        public static IFrameSource AddTo(GameObject host)
        {
#if UNITY_EDITOR
            return host.AddComponent<EditorTestFrameSource>();
#else
            // shortcut: no headset source yet, PassthroughFrameSource replaces this in phase 4
            Debug.LogError("[Cast] No frame source for this platform yet.");
            return null;
#endif
        }
    }
}
