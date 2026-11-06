using UnityEditor;
using UnityEngine;

namespace NWDL.Cast.EditorTools
{
    /// <summary>Adds or removes the passthrough components and project settings when usePassthrough of a CastStreamer is toggled.</summary>
    [InitializeOnLoad]
    internal static class PassthroughSetup
    {
        private const string ChildName = "Cast Passthrough";

        static PassthroughSetup() => CastStreamer.EditorApplyPassthrough = Apply;

        private static void Apply(CastStreamer cast, bool enable)
        {
            var child = cast.transform.Find(ChildName);
            if (enable && child == null)
            {
                var go = new GameObject(ChildName);
                Undo.RegisterCreatedObjectUndo(go, "Enable Cast passthrough");
                go.transform.SetParent(cast.transform, false);
                Undo.AddComponent<PassthroughFrameSource>(go); // brings PassthroughCameraAccess along
            }
            else if (!enable && child != null)
            {
                Undo.DestroyObjectImmediate(child.gameObject);
            }

            // The camera permission in the Android manifest needs both settings.
            var config = OVRProjectConfig.CachedProjectConfig;
            var supportMissing = enable && config.insightPassthroughSupport == OVRProjectConfig.FeatureSupport.None;
            if (config.isPassthroughCameraAccessEnabled == enable && !supportMissing) return;
            config.isPassthroughCameraAccessEnabled = enable;
            if (supportMissing) config.insightPassthroughSupport = OVRProjectConfig.FeatureSupport.Supported;
            OVRProjectConfig.CommitProjectConfig(config);
            Debug.Log($"[Cast] Passthrough camera access {(enable ? "enabled" : "disabled")} in the Meta project settings.");
        }
    }
}
