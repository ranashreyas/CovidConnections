'use strict';

// The Cloud Functions for Firebase SDK to create Cloud Functions and setup triggers.
const functions = require('firebase-functions');

// The Firebase Admin SDK to access the Firebase Realtime Database.
const admin = require('firebase-admin');

admin.initializeApp();

// Listens for updates to covid_diagnosed and updates the lastnotified timestamp.
exports.sendNotif = functions.database.ref('/app_data/devices/{deviceId}/nearby_devices')
    .onWrite(async(change, context) => {
      console.log('sendNotif', 'change: ', change);

      const payload = {
        notification: {
          title: 'Corona Virus ALERT!',
          body: 'You may be infected with Covid19!', 
        }
      };

      // Get the list of all the devices that are nearby the device with the given ID.
      // const getNearbyDevicesPromise = admin.database()
      //     .ref('/app_data/devices/{deviceId}/nearby_devices').once('value');

      // // The array containing all the devices' tokens.
      // const results = await Promise.all([getNearbyDevicesPromise]);

      const nearbyDevices = change.after.val();
      console.log('Nearby devices for ', context.params.deviceId, ', are ', nearbyDevices);

      if (nearbyDevices == null) {
        console.log('No new nearby devices for ', context.params.deviceId, ', will bypass notifications.');
        return admin.database().ref('app_data/lastnotified').set(context.timestamp);
      }

      nearbyDevices.forEach((nearbyDevice, index) => {
        console.log('Looking up ', nearbyDevice);
        console.log('Nearby device device id ', nearbyDevice.deviceId);

        const ref = change.after.ref.parent.ref.parent.child(nearbyDevice.deviceId);
        ref.once("value", function(snapshot) {
          var device = snapshot.val();
          const deviceToken = device.device_token;
          console.log('Sending notification to: ', deviceToken);

          // var deviceTokens;
          // deviceTokens.push(contents.device_token);

          // This should be moved out of the once.
          admin.messaging().sendToDevice(deviceToken, payload)
            .then(function(response) {
              console.log('Notification sent successfully:', response);
            })
            .catch(function(error) {
              console.log('Notification sent failed:', error);
            });
        });
      });

      // admin.messaging().sendToTopic("Covid19", payload)
      //   .then(function(response) {
      //     console.log('Notification sent successfully:', response);
      //   })
      //   .catch(function(error) {
      //     console.log('Notification sent failed:', error);
      // });

      // You must return a Promise when performing asynchronous tasks inside a Functions such as
      // writing to the Firebase Realtime Database.
      return admin.database().ref('app_data/lastnotified').set(context.timestamp);
    });