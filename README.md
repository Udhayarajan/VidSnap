# VidSnap Social Media Video Downloader App
VidSnap is an Android app that allows users to download videos from popular social media platforms such as Instagram, Facebook, Twitter, YouTube, Vimeo, DailyMotion, and Sharechat. This app is licensed under the GPL V3.0 license, and anyone who forks the project is requested to maintain the same license as per the law.

## Features
- Download videos from Instagram, Facebook, Twitter, YouTube, Vimeo, DailyMotion, and Sharechat
- Uses Java and Kotlin programming languages
- Utilizes FFmpeg-kit for merging audio and video
- Entirely customized directory for storage of downloaded file to database file

## Acknowledgments
We would like to thank the creators of  
- [FFmpeg-kit](https://github.com/tanersener/ffmpeg-kit)
- [Fetch](https://github.com/tonyofrancis/Fetch)
- [Glide](https://github.com/bumptech/glide)
- [VidSnapKit-Ultimate](https://github.com/Udhayarajan/VidSnapKit-Ultimate) [It's cloud API version also available for free]

Without these libraries, this project would not have been possible.

# Installation
To use VidSnap, you can download the APK from the [releases](https://github.com/Udhayarajan/VidSnap/releases/) page. Once downloaded, simply install the APK on your Android device, and you are good to go!

## Usage
Open the VidSnap app on your Android device
Enter the URL of the video you want to download
Click the download button
The video will be downloaded and saved in your device's gallery


## NOTE: 
- The commit message says FFmpeg is used with min-gpl but later after commit `.so` files are replaced with `full-gpl`

- If you build/modify this app of your own, please make sure that you are comply with [GNU GPL v3](https://www.gnu.org/licenses/gpl-3.0.en.html) Rules.

- If you plan to publish your own build in PlayStore, please remove `YouTube.java` & build, apk with static library loading of FFmpeg, by default this app uses dynamic loading of FFmpeg `.so` files to reduce apk size

## Contribution
 
 1. Fork it
 2. Modify it
 3. Create a pull request
 
 All contributions are welcomed...
 
 ## License
VidSnap Android App is licensed under the GPL V3.0 license. Any fork of this project is requested to maintain the same license as per the law. See the LICENSE file for more details..

## Sponsorship☕
[!["Buy Me A Coffee"](https://img.buymeacoffee.com/button-api/?text=Buy%20me%20a%20coffee&emoji=&slug=udhayarajan&button_colour=5F7FFF&font_colour=ffffff&font_family=Cookie&outline_colour=000000&coffee_colour=FFDD00)](https://www.buymeacoffee.com/udhayarajan)
