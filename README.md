Password Manager for Android (TREZOR)
======
Keep your passwords safe with Password Manager for TREZOR, the unofficial Android app for complete security. Password Manager for Android is not related with Satoshilabs nor [TREZOR](https://buytrezor.com/?a=227f182fcbbe).

More info at [Official website](https://pma.madriguera.me)


Available at [Google Playstore](https://play.google.com/store/apps/details?id=me.madriguera.pma)


## Requisites
* [Go](https://golang.org/) & [Gomobile](https://github.com/golang/go/wiki/Mobile)
* [Android NDK](https://developer.android.com/ndk/index.html)
* [Android SDK](https://developer.android.com/studio/index.html)
* [npm](https://www.npmjs.com/)
* Probably something more I can't remember

## Building the application
You first need to build the Go code (tesoroandroid/tesoroandroid.go) with
```bash
$ gomobile bind -target android -o tesoroandroid.aar -v .
```
and move the tesoroandroid.aar file to passwordmanager/android/tesoroandroid/tesoroandroid.aar


Then, build the Android application with gradle on the passwordmanager/android directory with 
```bash
$ ./gradlew assembleDebug && ./gradlew installDebug
```

## About the application

The UI is made with React Native, the main logic resides in a Go program that is compiled using gomobile bind command. Everything is glued together via gradle.
You need to set the Dropbox's API keys and path at tesoroandroid.go, DropBoxModule.java and in the AndroidManifest.xml file.
You need to set some other information, like your app's ID on some other files.


## Contributing to this project:

We worked hard and put a lot of effort in this app, we made it open source and available to anyone, so please, consider making a small donation and let us keep working adding new features: **1MVM8fBfkpxRqBADsYuvEHnMYgxRDWB99R**

If you find any improvement or issue you want to fix, feel free to send us a pull request or open an issue.




## License

This is distributed under the Apache License v2.0

Copyright 2017 F7U12 Team - pma@madriguera.me

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


                              

### Other licenses
Some parts, libraries and modules may have a different license, authors and copyright holders which is include along the library (Dropbox SDK (custom?, looks like MIT), json-simple (Apache v2.0), react-native-swipeout (MIT),...)