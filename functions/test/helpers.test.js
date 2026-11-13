// Unit tests for the pure helpers in index.js (review #18). Run: cd functions && npm test
const test = require("node:test");
const assert = require("node:assert");
const { startOfTodayIn, missingDropOff, busDocId } = require("../index.js")._test;

test("midnight in Johannesburg (UTC+2)", () => {
  const now = new Date("2026-11-13T15:30:00Z"); // 17:30 local
  assert.strictEqual(new Date(startOfTodayIn("Africa/Johannesburg", now)).toISOString(), "2026-11-12T22:00:00.000Z");
});

test("midnight just after the local date changes", () => {
  const now = new Date("2026-11-13T22:30:00Z"); // 00:30 on the 14th in Johannesburg
  assert.strictEqual(new Date(startOfTodayIn("Africa/Johannesburg", now)).toISOString(), "2026-11-13T22:00:00.000Z");
});

test("midnight in a zone west of UTC", () => {
  const now = new Date("2026-11-13T03:00:00Z"); // 22:00 on the 12th in New York (UTC-5)
  assert.strictEqual(new Date(startOfTodayIn("America/New_York", now)).toISOString(), "2026-11-12T05:00:00.000Z");
});

const since = 1_000;

test("picked up and dropped off is fine", () => {
  assert.strictEqual(missingDropOff({
    pickupStatus: { isPicked: true, timestamp: 2_000 },
    dropoffStatus: { isDropped: true, timestamp: 3_000 },
  }, since), false);
});

test("picked up with no drop-off is missing", () => {
  assert.strictEqual(missingDropOff({ pickupStatus: { isPicked: true, timestamp: 2_000 } }, since), true);
});

test("marked not dropped is missing", () => {
  assert.strictEqual(missingDropOff({
    pickupStatus: { isPicked: true, timestamp: 2_000 },
    dropoffStatus: { isDropped: false, timestamp: 3_000 },
  }, since), true);
});

test("yesterday's drop-off does not count for today's pick-up", () => {
  assert.strictEqual(missingDropOff({
    pickupStatus: { isPicked: true, timestamp: 2_000 },
    dropoffStatus: { isDropped: true, timestamp: 500 },
  }, since), true);
});

test("not picked up, or picked up before today, is not reported", () => {
  assert.strictEqual(missingDropOff({ pickupStatus: { isPicked: false, timestamp: 2_000 } }, since), false);
  assert.strictEqual(missingDropOff({ pickupStatus: { isPicked: true, timestamp: 500 } }, since), false);
  assert.strictEqual(missingDropOff({}, since), false);
});

test("bus document IDs match the app", () => {
  assert.strictEqual(busDocId(" KBX 1/2 "), "KBX 1_2");
  assert.strictEqual(busDocId(null), "");
});
