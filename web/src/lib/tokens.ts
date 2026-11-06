import { randomBytes } from 'node:crypto';
import { AccessToken, RoomServiceClient, TokenVerifier, TrackSource } from 'livekit-server-sdk';

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

/** True for a valid, unexpired viewer token of our room. */
export async function isViewerToken(keys: Keys, jwt: string): Promise<boolean> {
  try {
    const claims = await new TokenVerifier(keys.apiKey, keys.apiSecret).verify(jwt);
    return String(claims.sub).startsWith('viewer-') && claims.video?.room === ROOM;
  } catch {
    return false;
  }
}

/** Ends a device's cast by removing it from the room. False if the device is not in the room. */
export async function removeDevice(keys: Keys, livekitUrl: string, deviceId: string): Promise<boolean> {
  const api = new RoomServiceClient(livekitUrl.replace(/^ws/, 'http'), keys.apiKey, keys.apiSecret);
  try {
    await api.removeParticipant(ROOM, `device-${deviceId}`);
    return true;
  } catch {
    return false;
  }
}

export function viewerToken(keys: Keys): Promise<string> {
  const identity = `viewer-${randomBytes(4).toString('hex')}`;
  const token = new AccessToken(keys.apiKey, keys.apiSecret, { identity, ttl: TTL });
  token.addGrant({ room: ROOM, roomJoin: true, canPublish: false, canSubscribe: true, canPublishData: false });
  return token.toJwt();
}
