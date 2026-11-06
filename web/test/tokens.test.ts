import { test } from 'node:test';
import assert from 'node:assert/strict';
import { TokenVerifier } from 'livekit-server-sdk';
import { deviceToken, viewerToken } from '../src/lib/tokens.ts';

const keys = { apiKey: 'devkey', apiSecret: 'secret' };
const decode = (jwt: string) => new TokenVerifier(keys.apiKey, keys.apiSecret).verify(jwt);

test('device token has exactly the device grants', async () => {
  const claims = await decode(await deviceToken(keys, 'quest01'));
  assert.equal(claims.sub, 'device-quest01');
  assert.deepEqual(claims.video, {
    room: 'cast',
    roomJoin: true,
    canPublish: true,
    canPublishSources: ['camera', 'microphone', 'screen_share_audio'],
    canSubscribe: false,
    canPublishData: false,
  });
  assert.equal(Number(claims.exp) - Number(claims.nbf), 3600);
});

test('viewer token has exactly the viewer grants', async () => {
  const claims = await decode(await viewerToken(keys));
  assert.match(String(claims.sub), /^viewer-[0-9a-f]{8}$/);
  assert.deepEqual(claims.video, {
    room: 'cast',
    roomJoin: true,
    canPublish: false,
    canSubscribe: true,
    canPublishData: false,
  });
  assert.equal(Number(claims.exp) - Number(claims.nbf), 3600);
});
