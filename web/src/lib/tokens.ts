import { randomBytes } from 'node:crypto';
import { AccessToken, TrackSource } from 'livekit-server-sdk';

// One room for everything; the token decides what a participant may do in it.
export const ROOM = 'cast';
const TTL = '1h';

export type Keys = { apiKey: string; apiSecret: string };

export function deviceToken(keys: Keys, deviceId: string): Promise<string> {
  const token = new AccessToken(keys.apiKey, keys.apiSecret, { identity: `device-${deviceId}`, ttl: TTL });
  token.addGrant({
    room: ROOM,
    roomJoin: true,
    canPublish: true,
    // What the wearer sees as a screen share; app sound is mixed into the microphone track
    canPublishSources: [TrackSource.MICROPHONE, TrackSource.SCREEN_SHARE],
    canSubscribe: false,
    canPublishData: false,
  });
  return token.toJwt();
}

export function viewerToken(keys: Keys): Promise<string> {
  const identity = `viewer-${randomBytes(4).toString('hex')}`;
  const token = new AccessToken(keys.apiKey, keys.apiSecret, { identity, ttl: TTL });
  token.addGrant({ room: ROOM, roomJoin: true, canPublish: false, canSubscribe: true, canPublishData: false });
  return token.toJwt();
}
