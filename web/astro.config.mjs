import { defineConfig, envField } from 'astro/config';
import vercel from '@astrojs/vercel';

const secret = () => envField.string({ context: 'server', access: 'secret' });

export default defineConfig({
  adapter: vercel(),
  env: {
    schema: {
      LIVEKIT_API_KEY: secret(),
      LIVEKIT_API_SECRET: secret(),
      LIVEKIT_PUBLIC_URL: secret(),
      VIEWER_PASSWORD_HASH: secret(),
      DEVICES: secret(),
    },
  },
});
