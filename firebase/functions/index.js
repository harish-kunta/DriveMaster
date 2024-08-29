const functions = require("firebase-functions");
const admin = require("firebase-admin");

// Initialize Firebase Admin SDK
admin.initializeApp();

const db = admin.database();

exports.sendStreakNotifications = functions.pubsub.schedule("* * * * *").onRun(async () => {
    console.log('Fetching users from database...');
    const usersSnapshot = await db.ref("/users").once("value");
    const users = usersSnapshot.val();

    if (!users) {
        console.log('No users found in the database.');
        return null;
    }

    const promises = [];

    for (const userId in users) {
        const user = users[userId];
        console.log('User ' + userId + ' is at risk of losing streak.');

        const fcmToken = user.fcmToken;
        console.log('FCMToken ' + fcmToken);

        if (fcmToken) {
            const message = {
                notification: {
                    title: "Don't lose your streak!",
                    body: "Complete a lesson today to keep your streak going.",
                },
                token: fcmToken,
            };
            console.log('Sending notification to user ' + userId);
            promises.push(admin.messaging().send(message));
        }
    }

    await Promise.all(promises);
    return null;
});
