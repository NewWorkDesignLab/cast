import type { APIRoute } from 'astro';
import { LIVEKIT_PUBLIC_URL } from 'astro:env/server';
import { DEVICE_ID } from '../../../lib/auth';
import { badRequest, isViewer, keys, unauthorized } from '../../../lib/server';
import { removeDevice } from '../../../lib/tokens';

export const prerender = false;

// A signed-in viewer ends a headset's cast. Starting stays on the headset.
export const POST: APIRoute = async ({ request }) => {
  if (!(await isViewer(request))) return unauthorized();
  const body = await request.json().catch(() => null);
  const deviceId = body?.deviceId;
  if (typeof deviceId !== 'string' || !DEVICE_ID.test(deviceId)) return badRequest();
  if (!(await removeDevice(keys, LIVEKIT_PUBLIC_URL, deviceId))) {
    return Response.json({ error: 'not casting' }, { status: 404 });
  }
  return Response.json({ ok: true });
};
