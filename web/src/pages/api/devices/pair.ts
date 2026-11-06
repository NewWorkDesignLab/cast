import type { APIRoute } from 'astro';
import { randomBytes } from 'node:crypto';
import { sha256Hex } from '../../../lib/auth';
import { badRequest, store } from '../../../lib/server';

export const prerender = false;

// The headset exchanges the one-time code from the QR code for permanent device credentials.
export const POST: APIRoute = async ({ request }) => {
  const body = await request.json().catch(() => null);
  const code = body?.code;
  if (typeof code !== 'string' || code.length > 64) return badRequest();

  const name = await store.takePairing(code);
  if (!name) return Response.json({ error: 'invalid or expired code' }, { status: 410 });

  const deviceId = randomBytes(4).toString('hex');
  const deviceSecret = randomBytes(32).toString('base64url');
  await store.putDevice({ deviceId, name, secretSha256: sha256Hex(deviceSecret), createdAt: new Date().toISOString() });
  return Response.json({ deviceId, deviceSecret, name });
};
