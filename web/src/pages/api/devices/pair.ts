import type { APIRoute } from 'astro';
import { randomBytes } from 'node:crypto';
import { deviceCode, deviceName, sha256Hex } from '../../../lib/auth';
import { badRequest, store } from '../../../lib/server';

export const prerender = false;

// The headset exchanges the one-time code from the QR code for permanent device credentials.
// Its name is its model plus a random code, e.g. "Quest 3 - K7M2QX"; the code is also its device ID.
export const POST: APIRoute = async ({ request }) => {
  const body = await request.json().catch(() => null);
  const code = body?.code;
  if (typeof code !== 'string' || code.length > 64) return badRequest();
  if (!(await store.takePairing(code))) return Response.json({ error: 'invalid or expired code' }, { status: 410 });

  let deviceId = deviceCode();
  while (await store.getDevice(deviceId)) deviceId = deviceCode();
  const name = deviceName(body.model, deviceId);
  const deviceSecret = randomBytes(32).toString('base64url');
  await store.putDevice({ deviceId, name, secretSha256: sha256Hex(deviceSecret), createdAt: new Date().toISOString() });
  return Response.json({ deviceId, deviceSecret, name });
};
