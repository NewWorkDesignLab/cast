import type { APIRoute } from 'astro';
import { DEVICES, LIVEKIT_API_KEY, LIVEKIT_API_SECRET, LIVEKIT_PUBLIC_URL } from 'astro:env/server';
import { parseDevices, verifyDevice } from '../../../lib/auth';
import { deviceToken } from '../../../lib/tokens';

export const prerender = false;

export const POST: APIRoute = async ({ request }) => {
  const body = await request.json().catch(() => null);
  const deviceId = body?.deviceId;
  const deviceSecret = body?.deviceSecret;
  if (typeof deviceId !== 'string' || typeof deviceSecret !== 'string') {
    return Response.json({ error: 'bad request' }, { status: 400 });
  }
  if (!verifyDevice(parseDevices(DEVICES), deviceId, deviceSecret)) {
    return Response.json({ error: 'unauthorized' }, { status: 401 });
  }
  const token = await deviceToken({ apiKey: LIVEKIT_API_KEY, apiSecret: LIVEKIT_API_SECRET }, deviceId);
  return Response.json({ url: LIVEKIT_PUBLIC_URL, token });
};
