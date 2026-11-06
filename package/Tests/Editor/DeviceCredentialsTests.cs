using System;
using System.IO;
using System.Text.RegularExpressions;
using NUnit.Framework;
using UnityEngine;
using UnityEngine.TestTools;

namespace NWDL.Cast.Tests
{
    public class DeviceCredentialsTests
    {
        private string path;

        [SetUp]
        public void SetUp() => path = Path.Combine(Path.GetTempPath(), $"cast-{Guid.NewGuid()}.json");

        [TearDown]
        public void TearDown() => File.Delete(path);

        [Test]
        public void LoadsAValidFile()
        {
            File.WriteAllText(path, "{\"deviceId\":\"quest01\",\"deviceSecret\":\"s3cret\"}");
            Assert.IsTrue(DeviceCredentials.TryLoad(path, out var credentials));
            Assert.AreEqual("quest01", credentials.deviceId);
            Assert.AreEqual("s3cret", credentials.deviceSecret);
        }

        [Test]
        public void MissingFileDisablesStreaming()
        {
            LogAssert.Expect(LogType.Warning, new Regex("No device credentials"));
            Assert.IsFalse(DeviceCredentials.TryLoad(path, out var credentials));
            Assert.IsNull(credentials);
        }

        [Test]
        public void IncompleteFileIsRejected()
        {
            File.WriteAllText(path, "{\"deviceId\":\"quest01\"}");
            LogAssert.Expect(LogType.Error, new Regex("needs both deviceId and deviceSecret"));
            Assert.IsFalse(DeviceCredentials.TryLoad(path, out var credentials));
            Assert.IsNull(credentials);
        }

        [Test]
        public void BrokenJsonIsRejected()
        {
            File.WriteAllText(path, "not json");
            LogAssert.Expect(LogType.Error, new Regex("Cannot read"));
            Assert.IsFalse(DeviceCredentials.TryLoad(path, out _));
        }
    }
}
