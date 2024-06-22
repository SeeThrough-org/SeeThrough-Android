<h1 align="center"><img src="https://capsule-render.vercel.app/api?type=waving&height=300&color=gradient&text=SeeThrough&textBg=false&reversal=false&animation=fadeIn&desc=See%20Clearly,%20Act%20Swiftly&descAlign=49&descAlignY=62"></h1>

<p align="center"><sup>Real-Time Image and Video Dehazing for Mobile Devices**</sup></p>

---

## Overview

This guide provides steps to build the SeeThrough mobile application with OpenCV integration in Android Studio.

* [Building the Project](#building-the-project)
  1. [Importing OpenCV](#importing-opencv)
  2. [Installing the Application](#installing-the-application)

---

## Building the Project

### 1. Importing OpenCV

To integrate OpenCV into your Android Studio project, follow these steps:

1. **Download OpenCV Android SDK:**
   - Obtain the SDK from [OpenCV Releases](https://opencv.org/releases/).
   - Extract the downloaded archive.

2. **Import the OpenCV Module:**
   - In Android Studio, go to `File` > `New` > `Import Module`.
   - Select the `sdk` folder from the extracted OpenCV directory.
   - Click `Finish` to import the module into your project.

3. **Add OpenCV to Project Settings:**
   - Open the `settings.gradle` file in your project and include the OpenCV module:
     ```gradle
     include ':opencv'
     project(':opencv').projectDir = new File('path/to/opencv/sdk')
     ```
   - Adjust the path to match where you extracted the OpenCV SDK.

4. **Update App Module Dependencies:**
   - In `app/build.gradle`, add the OpenCV dependency:
     ```gradle
     dependencies {
         implementation project(':opencv')
         // other dependencies
     }
     ```

---

### 2. Installing the Application

Follow these steps to install the SeeThrough application:

1. **Download the Latest Release:**
   - Go to the [SeeThrough Releases](https://github.com/SeeThrough-org/SeeThrough-android/releases/) page.
   - Download the latest release of the application.

2. **Install on Your Device:**
   - Transfer the downloaded APK file to your Android device.
   - Open the APK file on your device and follow the on-screen instructions to install it.

---

### Additional Resources

- **OpenCV Documentation:** For more details on setting up OpenCV, visit the [OpenCV Documentation](https://docs.opencv.org/master/d5/da3/tutorial_py_setup_in_windows.html).
- **OpenCV Forum:** For community support, check out the [OpenCV Forum](https://forum.opencv.org/).

---

**SeeThrough**: **See Clearly, Act Swiftly**

---