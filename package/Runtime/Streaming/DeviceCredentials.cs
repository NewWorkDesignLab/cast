using System;
using System.IO;
using UnityEngine;

namespace NWDL.Cast
{
    /// <summary>
    /// Device ID and secret of this headset. They are provisioned per headset as cast.json in
    /// Application.persistentDataPath and never built into the app.
    /// </summary>
    [Serializable]
    public sealed class DeviceCredentials
    {
        public string deviceId;
        public string deviceSecret;

        public static string DefaultPath => Path.Combine(Application.persistentDataPath, "cast.json");

        /// <summary>
        /// Loads the credentials. Logs why and returns false if they are missing or invalid; the app keeps running without streaming.
        /// </summary>
        public static bool TryLoad(out DeviceCredentials credentials) => TryLoad(DefaultPath, out credentials);

        /// <summary>
        /// Loads the credentials. Logs why and returns false if they are missing or invalid; the app keeps running without streaming.
        /// </summary>
        public static bool TryLoad(string path, out DeviceCredentials credentials)
        {
            credentials = null;
            if (!File.Exists(path))
            {
                Debug.LogWarning($"[Cast] No device credentials at {path}, streaming is disabled. " +
                                 "Create them with 'npm run new-device -- <id>' in web/ and copy cast.json there (adb push).");
                return false;
            }

            DeviceCredentials loaded;
            try
            {
                loaded = JsonUtility.FromJson<DeviceCredentials>(File.ReadAllText(path));
            }
            catch (Exception e)
            {
                Debug.LogError($"[Cast] Cannot read {path}: {e.Message}");
                return false;
            }

            if (string.IsNullOrEmpty(loaded?.deviceId) || string.IsNullOrEmpty(loaded.deviceSecret))
            {
                Debug.LogError($"[Cast] {path} needs both deviceId and deviceSecret.");
                return false;
            }

            credentials = loaded;
            return true;
        }
    }
}
