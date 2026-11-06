using UnityEngine;

namespace NWDL.Cast
{
    /// <summary>Camera image the passthrough stream is built from, with the pose and lens it was taken with.</summary>
    public interface IFrameSource
    {
        /// <summary>False if the source can never deliver an image here, e.g. no headset over Meta Horizon Link in the Editor.</summary>
        bool IsAvailable { get; }

        /// <summary>Latest camera image, null until the source is ready. Pose and intrinsics are valid once this is set.</summary>
        Texture Texture { get; }

        /// <summary>World pose of the camera when the image was taken.</summary>
        Pose Pose { get; }

        /// <summary>Focal length (x, y) and principal point (z, w) as fractions of the image size; the principal point counts from the bottom left.</summary>
        Vector4 NormalizedIntrinsics { get; }
    }
}
