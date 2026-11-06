import type { APIRoute } from 'astro';
import { randomBytes } from 'node:crypto';
import { badRequest, isViewer, store, unauthorized } from '../../../lib/server';

export const prerender = false;

const TTL_SECONDS = 600;

// A signed-in viewer starts pairing a headset: a one-time code for the QR code, valid for 10 minutes.
export const POST: APIRoute = async ({ request }) => {
  if (!(await isViewer(request))) return unauthorized();
  const body = await request.json().catch(() => null);
  const name = typeof body?.name === 'string' ? body.name.trim() : '';
  if (!name || name.length > 40) return badRequest();

  const code = randomBytes(16).toString('base64url');
  await store.putPairing(code, name, TTL_SECONDS);
  // The headset reaches the server at the same origin the viewer uses
  const server = new URL(request.url).origin;
  return Response.json({ qr: JSON.stringify({ server, code }), expiresIn: TTL_SECONDS });
};
