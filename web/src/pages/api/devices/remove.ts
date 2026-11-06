import type { APIRoute } from 'astro';
import { LIVEKIT_PUBLIC_URL } from 'astro:env/server';
import { DEVICE_ID } from '../../../lib/auth';
import { badRequest, isViewer, keys, store, unauthorized } from '../../../lib/server';
import { removeDevice } from '../../../lib/tokens';

export const prerender = false;

// Unpairs a headset: its credentials stop working and a running cast ends.
export const POST: APIRoute = async ({ request }) => {
  if (!(await isViewer(request))) return unauthorized();
  const body = await request.json().catch(() => null);
  const deviceId = body?.deviceId;
  if (typeof deviceId !== 'string' || !DEVICE_ID.test(deviceId)) return badRequest();
  await store.deleteDevice(deviceId);
  await removeDevice(keys, LIVEKIT_PUBLIC_URL, deviceId);
  return Response.json({ ok: true });
};
