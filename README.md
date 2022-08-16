# Flutter Add2App Android module implemented with Feature Delivery

This is a Flutter [Add2App](https://docs.flutter.dev/development/add-to-app) Android module implemented with [Feature Delivery](https://developer.android.com/guide/playcore/feature-delivery). With this, we can achieve that our Android application can dynamically download an Android module that internally contains Flutter features.

## How to test it

### On our project
1. Clone repo.
2. In `my_flutter_module` run `flutter build aar`.
3. Open the Android Project, sync, switch variant to `release` and create a Bundle.
5. In the generated Bundle directory, run:
  ```bash
  bundletool build-apks --local-testing --bundle=app-release.aab --output=app-release.apks
  bundletool install-apks --apks=app-release.apks
  ```

### The App
* <ins>First screen</ins>:
  * Tap on the first button to simulate download the feature.
  * Wait a second (sometimes the process to simulate the installation needs it) and then tap on the second button to launch an *Activity* from the *Feature Module* through an *Intent*.
* <ins>Second screen</ins>:
  * This is an *Activity* that lives in the *Feature delivery module*. It contains a button for launch a *FlutterActivity*. Tap it. In *release* mode, we have an error.
* <ins>Third screen</ins>:
  * A *Flutter* screen.
 
