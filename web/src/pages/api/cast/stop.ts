import type { APIRoute } from 'astro';
import { LIVEKIT_API_KEY, LIVEKIT_API_SECRET, LIVEKIT_PUBLIC_URL } from 'astro:env/server';
import { DEVICE_ID } from '../../../lib/auth';
import { isViewerToken, removeDevice } from '../../../lib/tokens';

export const prerender = false;

// A signed-in viewer ends a headset's cast. Starting stays on the headset.
export const POST: APIRoute = async ({ request }) => {
  const keys = { apiKey: LIVEKIT_API_KEY, apiSecret: LIVEKIT_API_SECRET };
  const token = request.headers.get('Authorization')?.replace(/^Bearer /, '') ?? '';
  // shortcut: uses the viewer's join token (1 h); after that the viewer has to sign in again to end a cast
  if (!(await isViewerToken(keys, token))) return Response.json({ error: 'unauthorized' }, { status: 401 });

  const body = await request.json().catch(() => null);
  const deviceId = body?.deviceId;
  if (typeof deviceId !== 'string' || !DEVICE_ID.test(deviceId)) {
    return Response.json({ error: 'bad request' }, { status: 400 });
  }
  if (!(await removeDevice(keys, LIVEKIT_PUBLIC_URL, deviceId))) {
    return Response.json({ error: 'not casting' }, { status: 404 });
  }
  return Response.json({ ok: true });
};
