import type { APIRoute } from 'astro';
import { LIVEKIT_PUBLIC_URL } from 'astro:env/server';
import { isViewer, keys, store, unauthorized } from '../../../lib/server';
import { castingDevices } from '../../../lib/tokens';

export const prerender = false;

// Paired headsets for /headsets (without secrets), with whether they are casting right now.
export const GET: APIRoute = async ({ request }) => {
  if (!(await isViewer(request))) return unauthorized();
  const [devices, casting] = await Promise.all([store.listDevices(), castingDevices(keys, LIVEKIT_PUBLIC_URL)]);
  return Response.json(
    devices
      .map(({ deviceId, name, createdAt }) => ({ deviceId, name, createdAt, casting: casting.has(deviceId) }))
      .sort((a, b) => a.name.localeCompare(b.name)),
  );
};
