/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {setGlobalOptions} = require("firebase-functions");
const {onRequest} = require("firebase-functions/https");
const logger = require("firebase-functions/logger");

// For cost control, you can set the maximum number of containers that can be
// running at the same time. This helps mitigate the impact of unexpected
// traffic spikes by instead downgrading performance. This limit is a
// per-function limit. You can override the limit for each function using the
// `maxInstances` option in the function's options, e.g.
// `onRequest({ maxInstances: 5 }, (req, res) => { ... })`.
// NOTE: setGlobalOptions does not apply to functions using the v1 API. V1
// functions should each use functions.runWith({ maxInstances: 10 }) instead.
// In the v1 API, each function can only serve one request per container, so
// this will be the maximum concurrent request count.
setGlobalOptions({maxInstances: 10});

// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

// exports.helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });


const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const DATABASE_PATH = "users";

// Runs every day at 00:00 UTC
exports.dailyCleanup = functions.pubsub.schedule("every day 00:00").onRun(async (context) => {
  const now = Date.now();
  const threeDaysMillis = 3* 24* 60 * 60 * 1000;
  const cutoffTime = now- threeDaysMillis;

  const db = admin.database();
  const userRef = db.ref(DATABASE_PATH);

  try {
    const usersSnapshot = await userRef.once("value");
    const updates= {};

    usersSnapshot.forEach((userSnap) => {
      const stockData = userSnap.child("stock_data");
      stockData.forEach((stockItemSnap) => {
        const timestamp = stockItemSnap.child("timestamp").val();
        if (timestamp && timestamp < cutoffTime) {
          // Removes storeName field from stock item if older than 3 days.
          updates[`${userSnap.key}/stock_data/${stockItemSnap.key}/storeName`] = null;
        }
      });
    });

    if (Object.keys(updates).length > 0) {
      await userRef.update(updates);
      console.log(`Removed expired storeName fields from ${Object.keys(updates).length} items.`);
    } else {
      console.log("No expired storeName fields found.");
    }

    return null;
  } catch (error) {
    console.error("Error cleaning expired storeNames:", error);
    return null;
  }
});