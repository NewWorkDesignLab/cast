import { createHash, randomBytes, scrypt, timingSafeEqual } from 'node:crypto';
import { promisify } from 'node:util';

const scryptAsync = promisify(scrypt) as (password: string, salt: Buffer, keylen: number) => Promise<Buffer>;
const KEYLEN = 64;

export const DEVICE_ID = /^[A-Za-z0-9_-]{1,64}$/;

export type Device = { deviceId: string; secretSha256: string };

// Format scrypt:<saltHex>:<hashHex>. ':' instead of '$', because .env loaders expand '$'.
export async function hashPassword(password: string): Promise<string> {
  const salt = randomBytes(16);
  const hash = await scryptAsync(password, salt, KEYLEN);
  return `scrypt:${salt.toString('hex')}:${hash.toString('hex')}`;
}

export async function verifyPassword(password: string, stored: string): Promise<boolean> {
  const [scheme, saltHex, hashHex] = stored.split(':');
  const expected = Buffer.from(hashHex ?? '', 'hex');
  if (scheme !== 'scrypt' || !saltHex || expected.length !== KEYLEN) {
    throw new Error('VIEWER_PASSWORD_HASH has an invalid format, create it with npm run hash-password');
  }
  const actual = await scryptAsync(password, Buffer.from(saltHex, 'hex'), KEYLEN);
  return timingSafeEqual(actual, expected);
}

export function sha256Hex(value: string): string {
  return createHash('sha256').update(value).digest('hex');
}

export function parseDevices(json: string): Device[] {
  const devices = JSON.parse(json);
  const valid = Array.isArray(devices) && devices.every(
    (d) => DEVICE_ID.test(d?.deviceId) && /^[0-9a-f]{64}$/.test(d?.secretSha256),
  );
  if (!valid) throw new Error('DEVICES must be a JSON array of { deviceId, secretSha256 }');
  return devices;
}

export function verifyDevice(devices: Device[], deviceId: string, deviceSecret: string): boolean {
  const device = devices.find((d) => d.deviceId === deviceId);
  if (!device) return false;
  return timingSafeEqual(Buffer.from(sha256Hex(deviceSecret), 'hex'), Buffer.from(device.secretSha256, 'hex'));
}
