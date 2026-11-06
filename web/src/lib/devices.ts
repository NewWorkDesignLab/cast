import { randomBytes } from 'node:crypto';
import { deviceCode, deviceName, sha256Hex } from './auth.ts';
import type { Store } from './store.ts';

/**
 * Registers a headset from the setup script. The serial number identifies it: setting it up again keeps its
 * name and ID but replaces the secret, so old credentials stop working. New headsets are named
 * "<model> - <random code>", e.g. "Quest 3 - K7M2QX"; the code is also the device ID.
 */
export async function registerDevice(store: Store, serial: string, model: unknown) {
  const serialSha256 = sha256Hex(serial);
  const existing = (await store.listDevices()).find((d) => d.serialSha256 === serialSha256);
  let deviceId = existing?.deviceId ?? deviceCode();
  while (!existing && (await store.getDevice(deviceId))) deviceId = deviceCode();

  const deviceSecret = randomBytes(32).toString('base64url');
  const name = existing?.name ?? deviceName(model, deviceId);
  await store.putDevice({
    deviceId,
    name,
    secretSha256: sha256Hex(deviceSecret),
    serialSha256,
    createdAt: new Date().toISOString(),
  });
  return { deviceId, deviceSecret, name };
}
