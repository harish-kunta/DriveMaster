const functions = require('firebase-functions');
const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
admin.initializeApp();

const db = admin.database();

exports.sendStreakNotifications = functions.pubsub.schedule('* * * * *').onRun(async () => {
    console.log('Fetching users from database...');

    try {
        const usersSnapshot = await db.ref('/users').once('value');
        const users = usersSnapshot.val();

        if (!users) {
            console.log('No users found in the database.');
            return null;
        }

        console.log(`Found ${Object.keys(users).length} users.`);
        const promises = [];

        for (const userId in users) {
            const user = users[userId];
            console.log('Processing user ', userId);

            const fcmToken = user.fcmToken;
            if (!fcmToken) {
                console.log('No FCM token found for user ', userId);
                continue;
            }

            console.log('FCM token for user ', userId, fcmToken);

            const message = {
                data: {
                    title: "Don't lose your streak!",
                    body: "Complete a lesson today to keep your streak going.",
                },
                token: fcmToken,
            };

            console.log('Sending notification to user ', userId);
            promises.push(admin.messaging().send(message)
                .catch(error => {
                    console.error('Failed to send notification to user', userId, error);
                }));
        }

        // Wait for all notifications to be sent
        await Promise.all(promises);
        console.log('All notifications processed.');

        return null;

    } catch (error) {
        console.error('Error fetching users or sending notifications:', error);
        throw new functions.https.HttpsError('internal', 'Failed to send notifications.');
    }
});
