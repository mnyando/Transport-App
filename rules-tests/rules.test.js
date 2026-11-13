// Checks ../firestore.rules in the Firestore emulator (review #2 and #18).
//   cd rules-tests && npm ci && npm test
// Needs Java 11+ on the PATH for the emulator. RULES_FILE overrides the rules path.
const fs = require("fs");
const path = require("path");
const { initializeTestEnvironment, assertSucceeds, assertFails } = require("@firebase/rules-unit-testing");
const {
  doc, getDoc, setDoc, updateDoc, addDoc, getDocs, collection, collectionGroup, query, where, orderBy, limit,
  serverTimestamp,
} = require("firebase/firestore");

const RULES = process.env.RULES_FILE || path.join(__dirname, "..", "firestore.rules");
const INVITE = "WELCOME1";
let env;
let passed = 0;
let failed = 0;

async function check(name, p) {
  try {
    await p;
    passed++;
    console.log("  ok   " + name);
  } catch (e) {
    failed++;
    console.log("  FAIL " + name + "\n       " + ((e && e.message) || e));
  }
}

const parentA = { email: "parent.a@example.com", email_verified: true };
const parentAUnverified = { email: "parent.a@example.com", email_verified: false };
const parentB = { email: "parent.b@example.com", email_verified: true };
const driver1 = { email: "driver1@example.com", email_verified: true };

(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-transport",
    firestore: { rules: fs.readFileSync(RULES, "utf8"), host: "127.0.0.1", port: 8080 },
  });

  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, "config/signup"), { inviteCode: INVITE });
    await setDoc(doc(db, "users/school1"), { role: "school", email: "office@example.com" });
    await setDoc(doc(db, "users/parentA"), { role: "parent", email: parentA.email });
    await setDoc(doc(db, "users/parentAU"), { role: "parent", email: parentA.email });
    await setDoc(doc(db, "users/parentB"), { role: "parent", email: parentB.email });
    await setDoc(doc(db, "users/driver1"), { role: "driver", email: driver1.email, staffId: "LIC1" });
    await setDoc(doc(db, "users/driverNoLink"), { role: "driver", email: "x@example.com" });
    await setDoc(doc(db, "staff/LIC1"), { staffName: "Dan", role: "Driver", assignedVehicle: "BUS 1", email: driver1.email });
    await setDoc(doc(db, "staff/LIC2"), { staffName: "Eve", role: "Driver", assignedVehicle: "BUS 2" });
    // parentUids is written by Cloud Functions; parentAU is linked but has not verified its email.
    await setDoc(doc(db, "students/Grade 1/studentList/S1"),
      { name: "Amy", parentEmail: parentA.email, parentUids: ["parentA", "parentAU"], vehicle: "BUS 1" });
    await setDoc(doc(db, "students/Grade 2/studentList/S2"),
      { name: "Ben", parentEmail: parentB.email, parentUids: ["parentB"], vehicle: "BUS 2" });
    // Old record: right email but not linked yet. Email alone no longer gives access (review #1).
    await setDoc(doc(db, "students/Grade 2/studentList/S4"), { name: "Dee", parentEmail: parentA.email, vehicle: "BUS 2" });
    await setDoc(doc(db, "busGroups/BUS 1"), { vehicle: "BUS 1", parentEmails: [parentA.email], active: true, lat: 1, lng: 2 });
    await setDoc(doc(db, "busGroups/BUS 2"), { vehicle: "BUS 2", parentEmails: [parentB.email] });
    await setDoc(doc(db, "trips/T2"), { driverId: "LIC2", vehicle: "BUS 2", completed: false });
    await setDoc(doc(db, "notifications/N1"), { parentEmail: parentA.email, driverId: "LIC1", vehicle: "BUS 1", timestamp: 1 });
    await setDoc(doc(db, "notifications/N2"), { parentEmail: parentB.email, driverId: "LIC2", vehicle: "BUS 2", timestamp: 2 });
  });

  const as = (uid, token) => env.authenticatedContext(uid, token).firestore();
  const school = as("school1", { email: "office@example.com", email_verified: false });
  const pA = as("parentA", parentA);
  const pAU = as("parentAU", parentAUnverified);
  const d1 = as("driver1", driver1);
  const dNo = as("driverNoLink", { email: "x@example.com", email_verified: true });
  const anon = env.unauthenticatedContext().firestore();
  // A sign-up profile; pass a field as undefined to leave it out.
  const profile = (email, extra) => Object.fromEntries(Object.entries(
    { fullName: "N", email, role: "parent", consentVersion: 1, inviteCode: INVITE, ...extra })
    .filter(([, v]) => v !== undefined));

  console.log("Accounts");
  const newUser = as("newbie", { email: "new@example.com", email_verified: false });
  await check("sign-up creates own parent profile with consent and invite code", assertSucceeds(setDoc(doc(newUser, "users/newbie"),
    profile("new@example.com", { consentAt: serverTimestamp() }))));
  const evil = as("evil", { email: "evil@example.com" });
  await check("sign-up without the invite code is refused", assertFails(setDoc(doc(evil, "users/evil"),
    profile("evil@example.com", { inviteCode: undefined }))));
  await check("sign-up with a wrong invite code is refused", assertFails(setDoc(doc(evil, "users/evil"),
    profile("evil@example.com", { inviteCode: "GUESS123" }))));
  await check("sign-up cannot choose the school role", assertFails(setDoc(doc(evil, "users/evil"),
    profile("evil@example.com", { role: "school" }))));
  await check("sign-up without consent is refused", assertFails(setDoc(doc(evil, "users/evil"),
    profile("evil@example.com", { consentVersion: undefined }))));
  await check("sign-up cannot set its own staffId", assertFails(setDoc(doc(evil, "users/evil"),
    profile("evil@example.com", { staffId: "LIC2" }))));
  await check("sign-up cannot use someone else's email", assertFails(setDoc(doc(evil, "users/evil"),
    profile(parentA.email))));
  await check("parents cannot read the invite code", assertFails(getDoc(doc(pA, "config/signup"))));
  await check("school reads the invite code", assertSucceeds(getDoc(doc(school, "config/signup"))));
  await check("school changes the invite code", assertSucceeds(setDoc(doc(school, "config/signup"), { inviteCode: INVITE })));
  await check("parent cannot change the invite code", assertFails(setDoc(doc(pA, "config/signup"), { inviteCode: "MINE" })));
  await check("user cannot promote themselves", assertFails(updateDoc(doc(pA, "users/parentA"), { role: "school" })));
  await check("user can save own push token", assertSucceeds(updateDoc(doc(pA, "users/parentA"), { fcmTokens: ["t1"] })));
  await check("parent can save own alert settings", assertSucceeds(updateDoc(doc(pA, "users/parentA"), { alertPrefs: { pickup: false, dropoff: true } })));
  await check("alert settings must be true/false", assertFails(updateDoc(doc(pA, "users/parentA"), { alertPrefs: { pickup: "off" } })));
  await check("alert settings cannot add other keys", assertFails(updateDoc(doc(pA, "users/parentA"), { alertPrefs: { sos: false } })));
  await check("alert settings cannot sneak a role change", assertFails(updateDoc(doc(pA, "users/parentA"), { alertPrefs: { pickup: true }, role: "school" })));
  await check("cannot change another user's alert settings", assertFails(updateDoc(doc(pA, "users/parentB"), { alertPrefs: { pickup: false } })));
  await check("user cannot read another profile", assertFails(getDoc(doc(pA, "users/parentB"))));
  await check("signed-out visitor reads nothing", assertFails(getDoc(doc(anon, "students/Grade 1/studentList/S1"))));

  console.log("Children (linked by account ID, review #1)");
  const linkedTo = (db, uid) => getDocs(query(collectionGroup(db, "studentList"), where("parentUids", "array-contains", uid)));
  await check("parent lists own linked children", assertSucceeds(linkedTo(pA, "parentA")));
  await check("parent cannot list another family's children", assertFails(linkedTo(pA, "parentB")));
  await check("parent cannot read another child directly", assertFails(getDoc(doc(pA, "students/Grade 2/studentList/S2"))));
  await check("matching email alone no longer gives access", assertFails(getDoc(doc(pA, "students/Grade 2/studentList/S4"))));
  await check("old email query is refused", assertFails(getDocs(query(collectionGroup(pA, "studentList"),
    where("parentEmail", "==", parentA.email)))));
  await check("linked but unverified parent sees nothing", assertFails(linkedTo(pAU, "parentAU")));
  await check("parent cannot edit a child record", assertFails(updateDoc(doc(pA, "students/Grade 1/studentList/S1"), { vehicle: "BUS 2" })));
  await check("parent cannot link themselves to a child", assertFails(updateDoc(doc(pA, "students/Grade 2/studentList/S4"),
    { parentUids: ["parentA"] })));
  const byBus = (db, v) => getDocs(query(collectionGroup(db, "studentList"), where("vehicle", "==", v)));
  await check("driver lists children on own bus", assertSucceeds(byBus(d1, "BUS 1")));
  await check("driver cannot list another bus", assertFails(byBus(d1, "BUS 2")));
  await check("unlinked driver sees nothing", assertFails(byBus(dNo, "BUS 1")));
  await check("driver records pick-up status", assertSucceeds(updateDoc(doc(d1, "students/Grade 1/studentList/S1"),
    { currentTripId: "T1", pickupStatus: { isPicked: true } })));
  await check("driver cannot change the parent email", assertFails(updateDoc(doc(d1, "students/Grade 1/studentList/S1"),
    { parentEmail: "attacker@example.com" })));
  await check("driver cannot change parent links", assertFails(updateDoc(doc(d1, "students/Grade 1/studentList/S1"),
    { parentUids: ["driver1"] })));
  await check("school lists all children", assertSucceeds(getDocs(collectionGroup(school, "studentList"))));
  await check("school adds a child", assertSucceeds(setDoc(doc(school, "students/Grade 3/studentList/S3"),
    { name: "Cal", parentEmail: parentA.email, vehicle: "BUS 1" })));
  await check("school archives a removed child", assertSucceeds(setDoc(doc(school, "archivedStudents/S3"), { name: "Cal" })));
  await check("parent cannot read archived children", assertFails(getDoc(doc(pA, "archivedStudents/S3"))));

  console.log("Staff and trips");
  await check("parent cannot read staff records", assertFails(getDoc(doc(pA, "staff/LIC1"))));
  await check("driver reads own staff record", assertSucceeds(getDoc(doc(d1, "staff/LIC1"))));
  await check("driver cannot read other staff", assertFails(getDoc(doc(d1, "staff/LIC2"))));
  await check("driver cannot edit own staff record", assertFails(updateDoc(doc(d1, "staff/LIC1"), { assignedVehicle: "BUS 2" })));
  await check("driver starts a trip on own bus", assertSucceeds(setDoc(doc(d1, "trips/T1"),
    { driverId: "LIC1", vehicle: "BUS 1", completed: false, tripType: "pickup" })));
  await check("driver looks for own open trip (TripRepository query)", assertSucceeds(getDocs(query(collection(d1, "trips"),
    where("vehicle", "==", "BUS 1"), where("driverId", "==", "LIC1"), where("completed", "==", false)))));
  await check("driver cannot query trips without the driver filter", assertFails(getDocs(query(collection(d1, "trips"),
    where("vehicle", "==", "BUS 1"), where("completed", "==", false)))));
  await check("driver cannot start a trip as someone else", assertFails(setDoc(doc(d1, "trips/T9"), { driverId: "LIC2", vehicle: "BUS 2" })));
  await check("driver completes own trip with the children carried", assertSucceeds(updateDoc(doc(d1, "trips/T1"),
    { completed: true, studentIds: ["S1"] })));
  await check("driver closes an abandoned trip", assertSucceeds(updateDoc(doc(d1, "trips/T1"), { completed: true, abandoned: true })));
  await check("driver cannot touch another driver's trip", assertFails(updateDoc(doc(d1, "trips/T2"), { completed: true })));
  await check("driver writes trip status for a child", assertSucceeds(setDoc(doc(d1, "trips/T1/pickupStatus/S1"), { status: true })));
  await check("driver reads statuses to resume a trip", assertSucceeds(getDocs(collection(d1, "trips/T1/pickupStatus"))));
  await check("driver cannot write status on another trip", assertFails(setDoc(doc(d1, "trips/T2/pickupStatus/S2"), { status: true })));
  await check("parent cannot read trips", assertFails(getDoc(doc(pA, "trips/T1"))));

  console.log("Alerts");
  const notesFor = (db, email) => getDocs(query(collection(db, "notifications"), where("parentEmail", "==", email),
    orderBy("timestamp", "desc"), limit(50)));
  await check("parent lists own newest 50 alerts", assertSucceeds(notesFor(pA, parentA.email)));
  await check("parent cannot list another parent's alerts", assertFails(notesFor(pA, parentB.email)));
  await check("driver sends an alert for own bus", assertSucceeds(addDoc(collection(d1, "notifications"),
    { parentEmail: parentA.email, driverId: "LIC1", vehicle: "BUS 1" })));
  await check("driver cannot send as another driver", assertFails(addDoc(collection(d1, "notifications"),
    { parentEmail: parentB.email, driverId: "LIC2", vehicle: "BUS 2" })));
  await check("parent cannot create alerts", assertFails(addDoc(collection(pA, "notifications"),
    { parentEmail: parentB.email, driverId: "LIC1", vehicle: "BUS 1" })));
  await check("driver raises SOS for school", assertSucceeds(addDoc(collection(d1, "schoolAlerts"),
    { type: "sos", driverId: "LIC1", vehicle: "BUS 1" })));
  await check("parent cannot read school alerts", assertFails(getDocs(collection(pA, "schoolAlerts"))));

  console.log("Bus location and chat");
  await check("parent sees own child's bus", assertSucceeds(getDoc(doc(pA, "busGroups/BUS 1"))));
  await check("parent cannot see another bus", assertFails(getDoc(doc(pA, "busGroups/BUS 2"))));
  await check("driver shares location for own bus", assertSucceeds(setDoc(doc(d1, "busGroups/BUS 1"), { lat: 3, lng: 4, active: true }, { merge: true })));
  await check("driver cannot write another bus", assertFails(setDoc(doc(d1, "busGroups/BUS 2"), { lat: 3 }, { merge: true })));
  await check("parent cannot fake the bus location", assertFails(updateDoc(doc(pA, "busGroups/BUS 1"), { lat: 0 })));
  await check("parent reads own bus chat", assertSucceeds(getDocs(collection(pA, "busGroups/BUS 1/messages"))));
  await check("parent cannot read another bus chat", assertFails(getDocs(collection(pA, "busGroups/BUS 2/messages"))));
  await check("parent posts in own bus chat", assertSucceeds(addDoc(collection(pA, "busGroups/BUS 1/messages"),
    { senderUid: "parentA", text: "Hi" })));
  await check("chat message cannot impersonate", assertFails(addDoc(collection(pA, "busGroups/BUS 1/messages"),
    { senderUid: "driver1", text: "Hi" })));
  await check("driver posts in own bus chat", assertSucceeds(addDoc(collection(d1, "busGroups/BUS 1/messages"),
    { senderUid: "driver1", text: "On our way" })));
  await check("school reads any bus", assertSucceeds(getDoc(doc(school, "busGroups/BUS 2"))));

  console.log("Removed collections");
  await check("old lower-case routes collection is closed", assertFails(getDocs(collection(school, "routes"))));
  await check("old attendance records are closed", assertFails(getDocs(collection(school, "attendance/2026-11/records"))));

  await env.cleanup();
  console.log(`\n${passed} passed, ${failed} failed`);
  process.exit(failed ? 1 : 0);
})().catch((e) => {
  console.error(e);
  process.exit(1);
});
