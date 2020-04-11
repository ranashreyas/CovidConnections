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

      const newVal = change.after.val();
      if (newVal == null) {
        console.log('No new nearby devices for ', {deviceId}, ', will bypass notifications.');
        return admin.database().ref('app_data/lastnotified').set(context.timestamp);
      }

      var deviceIds;
      for(device in newVal){
        deviceIds.push(device['deviceId']);
      }


      console.log('Next from change', newVal);
      console.log('First value from change', newVal[0]);
      console.log('First deviceId', deviceIds[0]);


      // let nearbyDevices;
      // nearbyDevices = admin.database()
      //     .ref('/app_data/devices/{deviceId}/nearby_devices');

      // console.log('All tokens that need to be notified:', nearbyDevices);

      // let nds;
      // nds = nearbyDevices[0];

      // console.log('All nearby devices:', nds);

      admin.messaging().sendToTopic("Covid19", payload)
        .then(function(response) {
          console.log('Notification sent successfully:', response);
        })
        .catch(function(error) {
          console.log('Notification sent failed:', error);
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