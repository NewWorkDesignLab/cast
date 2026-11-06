import type { APIRoute } from 'astro';
import { LIVEKIT_PUBLIC_URL, VIEWER_PASSWORD_HASH } from 'astro:env/server';
import { isRememberToken, rememberToken, verifyPassword } from '../../../lib/auth';
import { badRequest, keys, REMEMBER_COOKIE, unauthorized } from '../../../lib/server';
import { viewerToken } from '../../../lib/tokens';

export const prerender = false;

// Sign-in with the password, or silently with the "stay signed in" cookie (empty body).
export const POST: APIRoute = async ({ request, cookies }) => {
  const body = await request.json().catch(() => null);
  const password = body?.password;
  if (password !== undefined) {
    if (typeof password !== 'string' || password.length > 256) return badRequest();
    if (!(await verifyPassword(password, VIEWER_PASSWORD_HASH))) return unauthorized();
    if (body.remember === true) {
      cookies.set(REMEMBER_COOKIE, rememberToken(keys.apiSecret, VIEWER_PASSWORD_HASH), {
        httpOnly: true,
        secure: import.meta.env.PROD,
        sameSite: 'strict',
        path: '/api/token',
        maxAge: 30 * 86_400,
      });
    }
  } else if (!isRememberToken(keys.apiSecret, VIEWER_PASSWORD_HASH, cookies.get(REMEMBER_COOKIE)?.value ?? '')) {
    return unauthorized();
  }
  return Response.json({ url: LIVEKIT_PUBLIC_URL, token: await viewerToken(keys) });
};
