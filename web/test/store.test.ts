import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { sha256Hex, verifyDevice } from '../src/lib/auth.ts';
import { registerDevice } from '../src/lib/devices.ts';
import { fileStore } from '../src/lib/store.ts';

const newStore = async () => fileStore(join(await mkdtemp(join(tmpdir(), 'cast-store-')), 'store.json'));

test('a new headset gets a name from its model and a random code', async () => {
  const store = await newStore();
  const device = await registerDevice(store, '2G0YC1ZF8G0CY3', 'Quest 3');
  assert.match(device.name, /^Quest 3 - [A-Z2-9]{6}$/);
  assert.equal(device.name.endsWith(device.deviceId), true);
  const stored = await store.getDevice(device.deviceId);
  assert.equal(stored?.serialSha256, sha256Hex('2G0YC1ZF8G0CY3'));
  assert.equal(verifyDevice([stored!], device.deviceId, device.deviceSecret), true);
});

test('setting up the same headset again keeps its name but replaces the secret', async () => {
  const store = await newStore();
  const first = await registerDevice(store, 'SERIAL1', 'Quest 3');
  const again = await registerDevice(store, 'SERIAL1', 'Quest 3');
  assert.equal(again.deviceId, first.deviceId);
  assert.equal(again.name, first.name);
  assert.notEqual(again.deviceSecret, first.deviceSecret);
  const stored = (await store.getDevice(first.deviceId))!;
  assert.equal(verifyDevice([stored], first.deviceId, first.deviceSecret), false);
  assert.equal(verifyDevice([stored], again.deviceId, again.deviceSecret), true);
  assert.equal((await store.listDevices()).length, 1);
});

test('different headsets get different devices', async () => {
  const store = await newStore();
  const a = await registerDevice(store, 'SERIAL1', 'Quest 3');
  const b = await registerDevice(store, 'SERIAL2', 'Quest 3S');
  assert.notEqual(a.deviceId, b.deviceId);
  assert.match(b.name, /^Quest 3S - /);
  assert.equal((await store.listDevices()).length, 2);
});
