using System;
using UnityEngine;

namespace NWDL.Cast
{
    /// <summary>
    /// Builds the stream image. Without passthrough it shows what the main camera sees; with passthrough it shows the
    /// headset camera image with the virtual content on top. Streaming itself (LiveKit) follows.
    /// </summary>
    [DisallowMultipleComponent]
    public sealed class CastStreamer : MonoBehaviour
    {
        [SerializeField] private CastSettings settings;

        [Tooltip("Stream the headset camera image with the virtual content on top. Needs the Meta XR SDK with MRUK 81 or later. " +
                 "Turning it on adds the passthrough components to the scene, turning it off removes them again. Off: the main camera is streamed.")]
        [SerializeField] private bool usePassthrough;

        [Tooltip("Optional renderer that shows the stream image, e.g. a quad. While set, the image is rendered even when not streaming.")]
        [SerializeField] private Renderer debugView;

        private StreamCompositor compositor;
        private IFrameSource source;
        private float nextFrameTime;

        public bool UsePassthrough => usePassthrough;

        /// <summary>The stream image, at the stream resolution.</summary>
        public RenderTexture Output => compositor?.Output;

        public bool IsStreaming { get; private set; }

        private void Start()
        {
            if (settings == null)
            {
                Debug.LogError("[Cast] No CastSettings assigned.", this);
                enabled = false;
                return;
            }

            compositor = new StreamCompositor(settings.streamResolution);
            if (usePassthrough) source = FindFrameSource();
            if (debugView != null) debugView.material.mainTexture = compositor.Output;
        }

        private void Update()
        {
            if (!IsStreaming && debugView == null) return;
            var now = Time.unscaledTime;
            if (now < nextFrameTime) return;
            nextFrameTime = Mathf.Max(nextFrameTime + 1f / settings.fps, now);

            var main = Camera.main;
            if (debugView != null) debugView.enabled = false; // keep the debug view out of its own image
            if (!usePassthrough)
            {
                if (main != null) compositor.RenderMainCamera(main);
            }
            else if (source != null)
            {
                compositor.RenderComposite(source, settings.virtualContentLayers, main);
            }
            if (debugView != null) debugView.enabled = true;
        }

        private void OnDestroy() => compositor?.Dispose();

        private IFrameSource FindFrameSource()
        {
            var found = GetComponentInChildren<IFrameSource>(true);
#if UNITY_EDITOR
            // Without a headset over Meta Horizon Link the Editor has no camera image.
            if (found == null || !found.IsAvailable) return gameObject.AddComponent<EditorTestFrameSource>();
#endif
            if (found == null) Debug.LogError("[Cast] usePassthrough is on, but the passthrough components are missing. Turn it off and on again in the Inspector.", this);
            return found;
        }

#if UNITY_EDITOR
        /// <summary>Set by the optional passthrough editor assembly: adds (true) or removes (false) the passthrough components.</summary>
        public static Action<CastStreamer, bool> EditorApplyPassthrough;

        [SerializeField, HideInInspector] private bool passthroughApplied;

        private void OnValidate()
        {
            if (usePassthrough == passthroughApplied) return;
            UnityEditor.EditorApplication.delayCall += ApplyPassthrough;
        }

        private void ApplyPassthrough()
        {
            if (this == null || Application.isPlaying || UnityEditor.EditorUtility.IsPersistent(this) || usePassthrough == passthroughApplied) return;
            if (EditorApplyPassthrough == null && usePassthrough)
            {
                Debug.LogError("[Cast] usePassthrough needs the Meta XR SDK with MRUK 81 or later (com.meta.xr.mrutilitykit).", this);
                usePassthrough = false;
            }
            else
            {
                EditorApplyPassthrough?.Invoke(this, usePassthrough);
            }
            passthroughApplied = usePassthrough;
            UnityEditor.EditorUtility.SetDirty(this);
        }
#endif
    }
}
