using System;
using UnityEngine;
using UnityEngine.Rendering;
using Object = UnityEngine.Object;

namespace NWDL.Cast
{
    /// <summary>Renders the stream image: the main camera's view, or the camera image with the virtual content on top.</summary>
    internal sealed class StreamCompositor : IDisposable
    {
        private static readonly int VirtualTex = Shader.PropertyToID("_VirtualTex");

        private readonly Camera spectator;
        private readonly RenderTexture virtualLayer;
        private readonly Material composite;

        public RenderTexture Output { get; }

        public StreamCompositor(Vector2Int size)
        {
            Output = new RenderTexture(size.x, size.y, 24) { name = "Cast output" };
            virtualLayer = new RenderTexture(size.x, size.y, 24) { name = "Cast virtual layer" };
            composite = new Material(Resources.Load<Shader>("NWDLCastComposite"));

            var go = new GameObject("Cast Spectator Camera") { hideFlags = HideFlags.HideAndDontSave };
            spectator = go.AddComponent<Camera>();
            spectator.enabled = false; // rendered on demand at stream fps, not every frame
        }

        /// <summary>Renders what the main camera sees.</summary>
        public void RenderMainCamera(Camera main)
        {
            spectator.CopyFrom(main);
            Prepare(main.transform.position, main.transform.rotation);
            spectator.aspect = (float)Output.width / Output.height;
            spectator.ResetProjectionMatrix();
            Render(Output);
        }

        /// <summary>Renders the virtual content from the camera's pose and lens and lays it over the camera image.</summary>
        public void RenderComposite(IFrameSource source, LayerMask virtualContent, Camera main)
        {
            var image = source.Texture;
            if (image == null) return;

            var pose = source.Pose;
            Prepare(pose.position, pose.rotation);
            spectator.clearFlags = CameraClearFlags.SolidColor;
            spectator.backgroundColor = Color.clear;
            spectator.cullingMask = virtualContent;
            if (main != null)
            {
                spectator.nearClipPlane = main.nearClipPlane;
                spectator.farClipPlane = main.farClipPlane;
            }
            spectator.projectionMatrix = Projection(source.NormalizedIntrinsics, spectator.nearClipPlane, spectator.farClipPlane);
            Render(virtualLayer);

            composite.SetTexture(VirtualTex, virtualLayer);
            Graphics.Blit(image, Output, composite);
        }

        public void Dispose()
        {
            Object.Destroy(spectator.gameObject);
            Object.Destroy(composite);
            virtualLayer.Release();
            Output.Release();
        }

        private void Prepare(Vector3 position, Quaternion rotation)
        {
            spectator.enabled = false;
            spectator.transform.SetPositionAndRotation(position, rotation);
        }

        // A camera with a target texture always renders mono, also while XR is running.
        private void Render(RenderTexture target)
        {
            spectator.targetTexture = target;
            var request = new RenderPipeline.StandardRequest { destination = target };
            if (GraphicsSettings.currentRenderPipeline != null && RenderPipeline.SupportsRenderRequest(spectator, request))
                RenderPipeline.SubmitRenderRequest(spectator, request);
            else
                spectator.Render(); // built-in render pipeline
            spectator.targetTexture = null;
        }

        // Pinhole projection from normalized intrinsics, in Unity's view space (camera looks along -z).
        private static Matrix4x4 Projection(Vector4 k, float near, float far)
        {
            var m = Matrix4x4.zero;
            m[0, 0] = 2 * k.x;
            m[0, 2] = 1 - 2 * k.z;
            m[1, 1] = 2 * k.y;
            m[1, 2] = 1 - 2 * k.w;
            m[2, 2] = -(far + near) / (far - near);
            m[2, 3] = -2 * far * near / (far - near);
            m[3, 2] = -1;
            return m;
        }
    }
}
