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
