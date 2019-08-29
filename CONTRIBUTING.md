# Contributing

There are a couple of ways to contribute to Aegis. This document contains some
general guidelines for each type of contribution.

## Pull requests

If you're planning on adding a new feature or making other large changes, please
discuss it with us first through a proposal on GitHub. Discussing your idea with
us first ensures that everyone is on the same page before you start working on
your change. We don't like rejecting pull requests.

## Bug reports

We use GitHub's issue tracker to track bugs. Bug reports must follow [the
template](.github/ISSUE_TEMPLATE/bug.md). If a bug report does not follow the
template and does not contain enough information, it will be closed without a
response. Duplicate bug reports receive the same treatment.

Please consider trying to find the root cause yourself first and perhaps even
send us a patch that fixes the issue! We're happy to help you if you get stuck
along the way.

### Capturing a log with ADB

In some cases, we ask our users to obtain a debug log from their device. This is
typically only necessary if Aegis:
- Is unable to recover from an error and crashes.
- Only shows a generic error to the user, but writes a more detailed one to the
  log.

Capturing a log with the Android Debug Bridge (ADB) allows us to see the stack
trace and the exception that occurred.

#### Preparation

Before you can capture a log, you first need to go through a one-time setup
process on your Android device and computer.

##### Prerequisites

- Your Android device.
- A computer with Windows, Mac or Linux.
- A USB cable to connect your Android device to your computer.

##### Setup

__On your Android device__:

1. Navigate to ``Settings -> About``, scroll down and start tapping on the build
   number until developer options are enabled.
2. Navigate to ``Settings -> System -> Developer options`` and enable ``USB
   debugging``.

These navigation steps may differ slightly across Android versions and ROMs.

__On your computer__:

3. Download and extract the SDK platform tools for Android:
   https://developer.android.com/studio/releases/platform-tools.
4. Start your terminal emulator (If you're on Windows, start PowerShell) and
   navigate to the folder where platform-tools was extracted.
5. Execute ``adb devices``.

__On your Android device__:

6. A prompt will appear. Select "Always allow from this computer" and accept the
   connection.

#### Capturing a log

__On your Android device__:

1. Start Aegis.

__On your PC__:

2. Start your terminal emulator (If you're on Windows, start PowerShell) and
   navigate to the folder where platform-tools was extracted.
3. Start a log capture by executing the following commands.

    ```
    adb logcat -c
    adb logcat -f debug.log
    ```

    The logcat command captures the full system log by default, which may expose
    some sensitive information. While this information can sometimes help with
    finding the root cause of the issue, it is not always necessary. To only
    capture the log output of Aegis, replace the last logcat command with the
    one below:

    ```sh
    adb logcat --pid=$(adb shell pidof -s com.beemdevelopment.aegis) -f debug.log
    ```
    _If you are using a debug APK, replace ``com.beemdevelopment.aegis`` with
   ``com.beemdevelopment.aegis.debug``._

__On your Android device__:

4. Reproduce the issue.

__On your PC__:

5. Stop the log capture with Ctrl+C.
6. Attach the ``debug.log`` file to your issue on GitHub.
