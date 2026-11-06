import type { APIRoute } from 'astro';
import { REMEMBER_COOKIE } from '../../../lib/server';

export const prerender = false;

// Forgets "stay signed in"; the page drops its token itself.
export const POST: APIRoute = ({ cookies }) => {
  cookies.delete(REMEMBER_COOKIE, { path: '/api/token' });
  return Response.json({ ok: true });
};
