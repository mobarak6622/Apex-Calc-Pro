const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write calculations", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("calculations").get()
  );
});

test("Authenticated user: can create and query own calculation with matching filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("calculations").doc("calc_1").set({
      userId: ALICE_UID,
      workspaceId: "ws_main",
      title: "Euler Identity",
      expression: "sin(pi/2) + cos(0)",
      result: "2",
      encryptedPayload: "AES256GCM:encrypted_blob",
      isPinned: true,
      category: "SCIENTIFIC",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("calculations")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's calculations", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("calculations").doc("calc_secret").set({
      userId: ALICE_UID,
      workspaceId: "ws_main",
      title: "Secret Formula",
      expression: "42 * 10",
      result: "420",
      encryptedPayload: "AES256GCM:secret",
      isPinned: false,
      category: "STANDARD",
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("calculations").doc("calc_secret").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("calculations").where("userId", "==", ALICE_UID).get()
  );
});

test("Shadow Update & Ghost Field rejection: rejects unknown fields on create/update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("workspaces").doc("ws_1").set({
      userId: ALICE_UID,
      name: "Workspace 1",
      currentExpression: "2+2",
      currentResult: "4",
      memoryValue: "0",
      angleMode: "DEG",
      accentColorHex: "#00E5FF",
      isAdminHack: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated user: can create and list own workspaces and cloud backups", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("workspaces").doc("ws_1").set({
      userId: ALICE_UID,
      name: "Engineering Window",
      currentExpression: "sqrt(144)",
      currentResult: "12",
      memoryValue: "12",
      angleMode: "RAD",
      accentColorHex: "#00E5FF",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("backups").doc("bk_1").set({
      userId: ALICE_UID,
      backupName: "Auto Encrypted Vault",
      providerTag: "FIREBASE_CLOUD",
      encryptedData: "AES256_CIPHERTEXT_PAYLOAD",
      checksumSha256: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      itemCount: 5,
      createdAt: now,
      updatedAt: now,
    })
  );
});
