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

  const tokenOwners = new Map(); // token -> user doc ref
  for (const doc of users.docs) {
    if ((doc.get("role") || "").toLowerCase() !== "parent") continue;
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
