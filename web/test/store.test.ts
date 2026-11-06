import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileStore } from '../src/lib/store.ts';

const newStore = async () => fileStore(join(await mkdtemp(join(tmpdir(), 'cast-store-')), 'store.json'));

test('a pairing code works exactly once', async () => {
  const store = await newStore();
  await store.putPairing('code1', 600);
  assert.equal(await store.takePairing('code1'), true);
  assert.equal(await store.takePairing('code1'), false);
  assert.equal(await store.takePairing('unknown'), false);
});

test('an expired pairing code is rejected', async () => {
  const store = await newStore();
  await store.putPairing('old', -1);
  assert.equal(await store.takePairing('old'), false);
});

test('devices can be added, listed and removed', async () => {
  const store = await newStore();
  const device = { deviceId: 'a1b2c3d4', name: 'Quest 1', secretSha256: 'x'.repeat(64), createdAt: '2026-11-06T00:00:00Z' };
  await store.putDevice(device);
  assert.deepEqual(await store.getDevice('a1b2c3d4'), device);
  assert.deepEqual(await store.listDevices(), [device]);
  await store.deleteDevice('a1b2c3d4');
  assert.equal(await store.getDevice('a1b2c3d4'), null);
  assert.deepEqual(await store.listDevices(), []);
});
