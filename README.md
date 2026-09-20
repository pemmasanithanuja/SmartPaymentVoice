# Smart Payment Voice

An Android app that announces incoming UPI payments by voice, so you know who paid you and how much without looking at your phone.

> "500 rupees received from Rahul"

Everything runs on the device. There is no server, no database, and the app does not request internet permission.

## Features
- Listens to payment notifications (PhonePe, Google Pay, Paytm, BHIM) using a local `NotificationListenerService`
- Extracts the amount and the sender's name with regex and string processing
- Speaks the announcement with Android Text-to-Speech in English or Telugu
- Works with the screen locked
- Simple XML screen: notification access shortcut, voice on/off switch, language choice and a Test voice button
- Settings are stored locally in SharedPreferences

## Privacy
- Never reads or speaks phone numbers, UPI IDs, account numbers, OTPs or PINs. If the payer is shown only as a number or UPI ID, the app announces the amount only
- Does not access contacts, location or the internet
- Ignores OTP, debit, request and failed-payment notifications
- Payment details are never saved or logged

## How it works
1. Android delivers each notification from the listed payment apps to `PaymentNotificationListener`
2. The listener filters out non-credit messages and OTP/debit text
3. Regex extracts the amount; the sender name comes from the notification title or text
4. Numbers and UPI IDs are rejected as names
5. The message is spoken with Text-to-Speech; duplicate notifications within 5 seconds are skipped

## Tech stack
Kotlin, Android Studio, NotificationListenerService, TextToSpeech, XML layouts, SharedPreferences. Min SDK 26 (Android 8.0).

## Setup
1. Install the APK on an Android phone
2. Open the app, tap "Open notification access settings" and turn on Smart Payment Voice
3. Use "Test voice" to check the voice and language

On some phones (vivo, Oppo, Xiaomi) also allow background activity and autostart for the app, otherwise announcements may stop when the screen is locked.

## Install over USB
1. On the phone, enable Developer options and USB debugging
2. Connect it to a computer that has ADB installed
3. Run: `adb install -r app-debug.apk`

## Limitations
- Android only. iOS does not let an app read other apps' notifications
- Tested with PhonePe notifications on Android 13. Other apps' notification formats may need parser changes
- The Telugu voice needs the Telugu voice data from Google Text-to-Speech installed on the phone
