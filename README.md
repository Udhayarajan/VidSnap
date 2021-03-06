# VidSnap Social Media video downloader

This app is used to download video from social media platform.

- If you build/modify this app of your own, please make sure that you are comply with [GNU GPL v3](https://www.gnu.org/licenses/gpl-3.0.en.html) Rules.

- If you plan to publish your own build in PlayStore, please remove [YouTube.java](https://github.com/Udhayarajan/VidSnap/blob/16dc24a5d649edca39d1ac6dae0e60b0e68126f7/app/src/main/java/com/mugames/vidsnap/Extractor/YouTube.java#L38) & build, apk with static library loading, by default this app uses dynamic loading to reduce apk size,

Refer following lines to build as static library loading:

  - [Reference 1](https://github.com/Udhayarajan/VidSnap/blob/16dc24a5d649edca39d1ac6dae0e60b0e68126f7/app/src/main/java/com/mugames/vidsnap/ui/main/Activities/MainActivity.java#L616)
  
  - [Reference 2](https://github.com/Udhayarajan/VidSnap/blob/16dc24a5d649edca39d1ac6dae0e60b0e68126f7/app/src/main/java/com/mugames/vidsnap/Threads/Downloader.java#L280)
  
  - [Reference 3](https://github.com/Udhayarajan/VidSnap/blob/16dc24a5d649edca39d1ac6dae0e60b0e68126f7/settings.gradle#L1)
 
 ## Contribution
 
 1. Fork it
 2. Modify it
 3. Open an issue, so let's discuss about your modification for better clarity
 4. Create a pull request
 
 All contributions are welcomed...
 
 
 
### Additional Library used
- [FFmpeg-kit](https://github.com/tanersener/ffmpeg-kit)
- [Fetch](https://github.com/tonyofrancis/Fetch)
- [Glide](https://github.com/bumptech/glide)


