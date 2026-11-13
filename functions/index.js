/**
 * Push alerts for the Transport App.
 *
 * The Android app never sends push messages itself (the old upstream
 * FirebaseMessaging.send() API was shut down and could not reach other phones).
 * Instead the driver's phone writes a document to `notifications`, and this
 * function delivers it with the FCM HTTP v1 API to every phone the parent is
 * signed in on (`users/{uid}.fcmTokens`, saved by PushTokens.java).
 *
 * Deploy (needs the Blaze plan): firebase deploy --only functions
 */
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const INVALID_TOKEN_CODES = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
  "messaging/invalid-argument",
]);

exports.sendParentPush = onDocumentCreated("notifications/{notificationId}", async (event) => {
  const snap = event.data;
  if (!snap) return;
  const n = snap.data();

  const parentEmail = (n.parentEmail || "").trim().toLowerCase();
  if (!parentEmail) {
    logger.warn("Notification has no parentEmail; nothing sent", { id: snap.id });
    return;
  }

  // Find the parent account(s) with this email. Only verified accounts get alerts,
  // matching the rule the app and firestore.rules use for reading child data.
  const users = await admin.firestore().collection("users")
    .where("email", "==", parentEmail)
    .get();

  // Parents may turn off the routine "picked up" / "dropped off" confirmations
  // (users/{uid}.alertPrefs, set in the app's alert settings). Alerts that a child was NOT
  // picked up or dropped off, and SOS alerts, are always sent.
  const routineType = n.status === "done" && (n.type === "pickup" || n.type === "dropoff")
    ? n.type : null;

  const tokenOwners = new Map(); // token -> user doc ref
  for (const doc of users.docs) {
    if ((doc.get("role") || "").toLowerCase() !== "parent") continue;
    if (routineType && (doc.get("alertPrefs") || {})[routineType] === false) continue;
    try {
      const authUser = await admin.auth().getUser(doc.id);
      if (!authUser.emailVerified) continue;
    } catch (err) {
      continue; // account deleted
    }
    for (const token of doc.get("fcmTokens") || []) {
      tokenOwners.set(token, doc.ref);
    }
  }

  const tokens = [...tokenOwners.keys()];
  if (tokens.length === 0) {
    logger.info("Parent has no registered devices", { id: snap.id });
    return;
  }

  const urgent = n.status && n.status !== "done";
  const response = await admin.messaging().sendEachForMulticast({
    tokens,
    notification: {
      title: n.title || "School transport",
      body: n.message || "",
    },
    data: {
      type: String(n.type || ""),
      status: String(n.status || ""),
      studentId: String(n.studentId || ""),
      tripId: String(n.tripId || ""),
      notificationId: snap.id,
    },
    android: {
      priority: "high",
      notification: {
        channelId: "trip_alerts",
        icon: "ic_notification",
        ...(urgent ? { defaultSound: true, visibility: "public" } : {}),
      },
    },
  });

  // Drop tokens for phones that uninstalled the app or signed out.
  const cleanups = [];
  response.responses.forEach((r, i) => {
    if (!r.success && r.error && INVALID_TOKEN_CODES.has(r.error.code)) {
      const ref = tokenOwners.get(tokens[i]);
      cleanups.push(ref.update({
        fcmTokens: admin.firestore.FieldValue.arrayRemove(tokens[i]),
      }));
    }
  });
  await Promise.all(cleanups);

  logger.info("Push sent", {
    id: snap.id,
    success: response.successCount,
    failure: response.failureCount,
  });
});

// ---------------------------------------------------------------------------
// Driver accounts (review section 3: "the role comes from the server account").
//
// A driver signs up in the app like anyone else (as a parent). When the school saves that
// person's login email on their staff record (role "Driver"), this links the two:
//   users/{uid}.role    = "driver"
//   users/{uid}.staffId = the staff document ID (licence number)
// firestore.rules then lets the driver see only the bus on that staff record. Removing the
// email, changing the role or deleting the staff record turns the account back into a
// parent account, so access ends straight away.
// ---------------------------------------------------------------------------
const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");

function normEmail(value) {
  return (value || "").trim().toLowerCase();
}

function isDriverStaff(data) {
  return !!data && (data.role || "").toLowerCase() === "driver"
    && (data.status || "Active") !== "Not Active" && !!normEmail(data.email);
}

async function unlinkStaff(db, staffId, keepUid) {
  const linked = await db.collection("users").where("staffId", "==", staffId).get();
  await Promise.all(linked.docs
    .filter((doc) => doc.id !== keepUid)
    .map((doc) => doc.ref.update({
      role: "parent",
      staffId: admin.firestore.FieldValue.delete(),
    })));
}

async function linkUser(db, uid, staffId) {
  await db.collection("users").doc(uid).set({ role: "driver", staffId }, { merge: true });
}

exports.linkStaffAccount = onDocumentWritten("staff/{staffId}", async (event) => {
  const db = admin.firestore();
  const staffId = event.params.staffId;
  const after = event.data && event.data.after.exists ? event.data.after.data() : null;

  if (!isDriverStaff(after)) {
    await unlinkStaff(db, staffId, null);
    return;
  }

  let authUser;
  try {
    authUser = await admin.auth().getUserByEmail(normEmail(after.email));
  } catch (err) {
    // The driver has not signed up yet; onUserProfileCreated links them when they do.
    await unlinkStaff(db, staffId, null);
    logger.info("No account yet for staff email", { staffId });
    return;
  }

  await unlinkStaff(db, staffId, authUser.uid);
  const profile = await db.collection("users").doc(authUser.uid).get();
  if (!profile.exists) return; // linked when the profile is created
  await linkUser(db, authUser.uid, staffId);
  logger.info("Linked driver account", { staffId });
});

exports.onUserProfileCreated = onDocumentCreated("users/{uid}", async (event) => {
  const db = admin.firestore();
  const email = normEmail(event.data && event.data.get("email"));
  if (!email) return;
  const staff = await db.collection("staff").where("email", "==", email).limit(5).get();
  const driver = staff.docs.find((doc) => isDriverStaff(doc.data()));
  if (!driver) return;
  await unlinkStaff(db, driver.id, event.params.uid);
  await linkUser(db, event.params.uid, driver.id);
  logger.info("Linked new driver account", { staffId: driver.id });
});

// ---------------------------------------------------------------------------
// Location retention (review section 3: "share the bus location only during an active trip").
// The app removes the position when a trip ends. If the driver's phone dies or loses signal
// mid-trip, this clears any position that has not been updated for STALE_LOCATION_MINUTES.
// ---------------------------------------------------------------------------
const STALE_LOCATION_MINUTES = 30;

exports.clearStaleBusLocations = onSchedule("every 15 minutes", async () => {
  const db = admin.firestore();
  const cutoff = Date.now() - STALE_LOCATION_MINUTES * 60 * 1000;
  const stale = await db.collection("busGroups")
    .where("active", "==", true)
    .where("updatedAtMillis", "<", cutoff)
    .get();
  const del = admin.firestore.FieldValue.delete();
  await Promise.all(stale.docs.map((doc) => doc.ref.update({
    active: false,
    lat: del,
    lng: del,
    accuracy: del,
    endedAt: admin.firestore.FieldValue.serverTimestamp(),
  })));
  if (stale.size) logger.info("Cleared stale bus locations", { count: stale.size });
});

// ---------------------------------------------------------------------------
// Shared push helper for the functions below.
// ---------------------------------------------------------------------------
const { onCall, HttpsError } = require("firebase-functions/v2/https");

/** Bus documents use the vehicle name with "/" replaced (BusGroups.docId in the app). */
function busDocId(vehicle) {
  return (vehicle || "").trim().replace("/", "_");
}

/** Sends one push to every device of the given user documents; drops dead tokens. */
async function pushToUsers(userDocs, title, body, data) {
  const owners = new Map();
  for (const doc of userDocs) {
    for (const token of doc.get("fcmTokens") || []) owners.set(token, doc.ref);
  }
  const tokens = [...owners.keys()];
  if (tokens.length === 0) return 0;
  const response = await admin.messaging().sendEachForMulticast({
    tokens,
    notification: { title, body },
    data: Object.fromEntries(Object.entries(data || {}).map(([k, v]) => [k, String(v ?? "")])),
    android: { priority: "high", notification: { channelId: "trip_alerts", icon: "ic_notification" } },
  });
  await Promise.all(response.responses.map((r, i) => {
    if (r.success || !r.error || !INVALID_TOKEN_CODES.has(r.error.code)) return null;
    return owners.get(tokens[i]).update({ fcmTokens: admin.firestore.FieldValue.arrayRemove(tokens[i]) });
  }));
  return response.successCount;
}

/** Verified parent accounts with this email. */
async function verifiedParentsByEmail(db, email) {
  if (!email) return [];
  const users = await db.collection("users").where("email", "==", email).get();
  const parents = [];
  for (const doc of users.docs) {
    if ((doc.get("role") || "").toLowerCase() !== "parent") continue;
    try {
      if ((await admin.auth().getUser(doc.id)).emailVerified) parents.push(doc);
    } catch (err) {
      // account deleted
    }
  }
  return parents;
}

// ---------------------------------------------------------------------------
// Parents linked to children by account ID (review #1).
// The school records the parent's email on the student. The link itself is the parent's UID in
// students/{grade}/studentList/{id}.parentUids, written here and only for a verified email.
// firestore.rules lets a parent read a child only through parentUids.
// ---------------------------------------------------------------------------

/** Called by the app when a parent opens their dashboard (ParentLinks.claim). */
exports.linkMyChildren = onCall(async (request) => {
  const auth = request.auth;
  if (!auth) throw new HttpsError("unauthenticated", "Sign in first.");
  if (auth.token.email_verified !== true) throw new HttpsError("permission-denied", "Verify your email first.");
  const db = admin.firestore();
  const profile = await db.collection("users").doc(auth.uid).get();
  if ((profile.get("role") || "").toLowerCase() !== "parent") return { linked: 0 };

  const email = normEmail(auth.token.email);
  const FieldValue = admin.firestore.FieldValue;
  const [matching, linked] = await Promise.all([
    db.collectionGroup("studentList").where("parentEmail", "==", email).get(),
    db.collectionGroup("studentList").where("parentUids", "array-contains", auth.uid).get(),
  ]);
  const writes = [];
  for (const doc of matching.docs) {
    if (!(doc.get("parentUids") || []).includes(auth.uid)) {
      writes.push(doc.ref.update({ parentUids: FieldValue.arrayUnion(auth.uid) }));
    }
  }
  // The school changed the parent email on a record: this account no longer has access.
  for (const doc of linked.docs) {
    if (normEmail(doc.get("parentEmail")) !== email) {
      writes.push(doc.ref.update({ parentUids: FieldValue.arrayRemove(auth.uid) }));
    }
  }
  await Promise.all(writes);
  return { linked: matching.size };
});

/**
 * Runs on every student change:
 *  - parentEmail added or changed: parentUids is reset to the verified parent accounts with that
 *    email (review #1);
 *  - bus changed, student added or removed: the bus's studentCount is recounted (review #13), so
 *    the capacity check in the app (StudentRecords) always starts from the true number.
 */
exports.onStudentWritten = onDocumentWritten("students/{grade}/studentList/{studentId}", async (event) => {
  const db = admin.firestore();
  const before = event.data.before.exists ? event.data.before.data() : null;
  const after = event.data.after.exists ? event.data.after.data() : null;
  const jobs = [];

  if (after) {
    const email = normEmail(after.parentEmail);
    const emailChanged = !before || normEmail(before.parentEmail) !== email;
    if (emailChanged || !Array.isArray(after.parentUids)) {
      jobs.push(verifiedParentsByEmail(db, email).then((parents) =>
        event.data.after.ref.update({ parentUids: parents.map((p) => p.id) })));
    }
  }

  const vehicles = new Set();
  if (!before || !after || before.vehicle !== after.vehicle) {
    if (before && before.vehicle) vehicles.add(before.vehicle);
    if (after && after.vehicle) vehicles.add(after.vehicle);
  }
  for (const vehicle of vehicles) {
    jobs.push((async () => {
      const count = (await db.collectionGroup("studentList").where("vehicle", "==", vehicle)
        .count().get()).data().count;
      const buses = await db.collection("vehicle").where("vehicleName", "==", vehicle).get();
      await Promise.all(buses.docs.map((doc) => doc.ref.update({ studentCount: count })));
    })());
  }
  await Promise.all(jobs);
});

// ---------------------------------------------------------------------------
// Driver and attendant names for parents (review #13). They used to be copied onto every child
// and went stale when staff changed; they now live on busGroups/{bus}, which parents can read.
// ---------------------------------------------------------------------------
exports.syncBusStaff = onDocumentWritten("staff/{staffId}", async (event) => {
  const db = admin.firestore();
  const before = event.data.before.exists ? event.data.before.data() : null;
  const after = event.data.after.exists ? event.data.after.data() : null;
  const vehicles = new Set([before && before.assignedVehicle, after && after.assignedVehicle].filter(Boolean));
  const del = admin.firestore.FieldValue.delete();

  await Promise.all([...vehicles].map(async (vehicle) => {
    const staff = await db.collection("staff").where("assignedVehicle", "==", vehicle).get();
    const active = staff.docs.map((d) => d.data()).filter((s) => (s.status || "Active") !== "Not Active");
    const driver = active.find((s) => (s.role || "").toLowerCase() === "driver");
    const attendant = active.find((s) => (s.role || "").toLowerCase() === "attendant");
    await db.collection("busGroups").doc(busDocId(vehicle)).set({
      vehicle,
      driverName: driver && driver.staffName ? driver.staffName : del,
      attendantName: attendant && attendant.staffName ? attendant.staffName : del,
    }, { merge: true });
  }));
});

// ---------------------------------------------------------------------------
// End-of-day check (review #3): any child picked up today but not dropped off raises an urgent
// alert to the school. Runs once each evening in the school's time zone.
// ---------------------------------------------------------------------------
const SCHOOL_TIME_ZONE = "Africa/Johannesburg"; // change to the school's time zone
const END_OF_DAY_SCHEDULE = "0 18 * * 1-5";      // 18:00 on school days

/** Milliseconds at local midnight today in the given IANA time zone. */
function startOfTodayIn(timeZone, now = new Date()) {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone, hourCycle: "h23", year: "numeric", month: "2-digit", day: "2-digit",
    hour: "2-digit", minute: "2-digit", second: "2-digit",
  }).formatToParts(now).reduce((acc, p) => ({ ...acc, [p.type]: p.value }), {});
  const wallClockAsUtc = Date.UTC(+parts.year, +parts.month - 1, +parts.day,
    +parts.hour, +parts.minute, +parts.second);
  const offset = wallClockAsUtc - Math.floor(now.getTime() / 1000) * 1000;
  return Date.UTC(+parts.year, +parts.month - 1, +parts.day) - offset;
}

/** True when the child was picked up at or after `since` and not dropped off since then. */
function missingDropOff(student, since) {
  const pick = student && student.pickupStatus;
  if (!pick || pick.isPicked !== true || !(pick.timestamp >= since)) return false;
  const drop = student.dropoffStatus;
  return !(drop && drop.isDropped === true && drop.timestamp >= pick.timestamp);
}

exports.endOfDayCheck = onSchedule({ schedule: END_OF_DAY_SCHEDULE, timeZone: SCHOOL_TIME_ZONE }, async () => {
  const db = admin.firestore();
  const since = startOfTodayIn(SCHOOL_TIME_ZONE);
  const day = new Date(since + 12 * 3600 * 1000).toISOString().slice(0, 10);
  const picked = await db.collectionGroup("studentList").where("pickupStatus.timestamp", ">=", since).get();
  const missing = picked.docs.filter((doc) => missingDropOff(doc.data(), since));
  if (missing.length === 0) return;

  const created = [];
  await Promise.all(missing.map(async (doc) => {
    const s = doc.data();
    const name = s.name || "A child";
    try {
      // One alert per child per day, even if the job runs twice.
      await db.collection("schoolAlerts").doc(`eod_${day}_${doc.id}`).create({
        type: "end_of_day",
        status: "not_dropped",
        studentId: doc.id,
        studentName: s.name || null,
        grade: doc.ref.parent.parent.id,
        vehicle: s.vehicle || null,
        message: `${name} was picked up today but has not been marked as dropped off. Check where they are now.`,
        resolved: false,
        timestamp: Date.now(),
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      created.push(name);
    } catch (err) {
      if (err.code !== 6) throw err; // 6 = ALREADY_EXISTS: alerted earlier today
    }
  }));
  if (created.length === 0) return;

  const school = await db.collection("users").where("role", "==", "school").get();
  await pushToUsers(school.docs, "Children not dropped off",
    created.length === 1 ? `${created[0]} was picked up today but not dropped off.`
      : `${created.length} children were picked up today but not dropped off.`,
    { type: "end_of_day" });
  logger.warn("End-of-day check: children not dropped off", { count: created.length });
});

// ---------------------------------------------------------------------------
// Bus chat push (review #11): a new message notifies everyone else in that bus's group —
// the parents in busGroups/{bus}.parentEmails and the driver linked to that bus.
// ---------------------------------------------------------------------------
exports.notifyChatMessage = onDocumentCreated("busGroups/{busId}/messages/{messageId}", async (event) => {
  const db = admin.firestore();
  const msg = event.data && event.data.data();
  if (!msg || !msg.text) return;
  const bus = await db.collection("busGroups").doc(event.params.busId).get();
  if (!bus.exists) return;
  const vehicle = bus.get("vehicle") || event.params.busId;

  const recipients = new Map();
  const emails = (bus.get("parentEmails") || []).map(normEmail).filter(Boolean);
  for (let i = 0; i < emails.length; i += 30) {
    const batch = await db.collection("users").where("email", "in", emails.slice(i, i + 30)).get();
    batch.docs.forEach((d) => recipients.set(d.id, d));
  }
  const staff = await db.collection("staff").where("assignedVehicle", "==", vehicle).get();
  const staffIds = staff.docs.map((d) => d.id);
  for (let i = 0; i < staffIds.length; i += 30) {
    const batch = await db.collection("users").where("staffId", "in", staffIds.slice(i, i + 30)).get();
    batch.docs.forEach((d) => recipients.set(d.id, d));
  }
  recipients.delete(msg.senderUid);

  const text = String(msg.text);
  const preview = text.length > 120 ? `${text.slice(0, 117)}...` : text;
  const sender = msg.senderName || (msg.senderRole === "driver" ? "Driver" : "Parent");
  await pushToUsers([...recipients.values()], `Bus ${vehicle}`, `${sender}: ${preview}`,
    { type: "chat", vehicle });
});

// Pure helpers, exported for the unit tests in functions/test.
exports._test = { startOfTodayIn, missingDropOff, busDocId };
