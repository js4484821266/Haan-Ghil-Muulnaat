# Privacy

Haan Ghil Muulnaat is designed as an on-device Android image-processing app.

## Image Handling

- The app lets the user select images from the Android gallery or share images from other apps.
- Selected images are processed locally on the device.
- The app does not implement an image upload endpoint or app-level image transfer path.
- Protected images are saved to the Android gallery only when the user requests saving or uses the auto-save share target.

## Android Permissions

The app may request permissions needed for:

- foreground background processing for auto-save jobs
- notifications for background job progress
- legacy external storage writes on Android versions where that permission is required

## Google Play Services ML Kit

The app uses Google Play Services ML Kit face detection and image labeling APIs for local evaluation. Depending on device state and Google Play Services behavior, ML Kit models may be delivered or updated by Google Play Services outside this app's own image-processing code.

## Network Behavior

The app source does not include a custom network client, image upload API, or remote analytics pipeline. If this changes, this document should be updated before release.

## Saved Output

Generated images are saved as PNG files in the user's gallery. On Android 10 and newer, they are written under the Pictures collection using the app's output folder.
