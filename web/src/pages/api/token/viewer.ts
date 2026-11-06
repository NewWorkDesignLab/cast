import type { APIRoute } from 'astro';
import { LIVEKIT_API_KEY, LIVEKIT_API_SECRET, LIVEKIT_PUBLIC_URL, VIEWER_PASSWORD_HASH } from 'astro:env/server';
import { verifyPassword } from '../../../lib/auth';
import { viewerToken } from '../../../lib/tokens';

export const prerender = false;

export const POST: APIRoute = async ({ request }) => {
  const body = await request.json().catch(() => null);
  const password = body?.password;
  if (typeof password !== 'string' || password.length > 256) {
    return Response.json({ error: 'bad request' }, { status: 400 });
  }
  if (!(await verifyPassword(password, VIEWER_PASSWORD_HASH))) {
    return Response.json({ error: 'unauthorized' }, { status: 401 });
  }
  const token = await viewerToken({ apiKey: LIVEKIT_API_KEY, apiSecret: LIVEKIT_API_SECRET });
  return Response.json({ url: LIVEKIT_PUBLIC_URL, token });
};
