# Aegis

Aegis is a free, secure and open source 2FA app for Android.

Aegis' security design and vault format is described in detail in [this
document](docs/vault.md).

# Features

- Free and open source
- Secure
  - Encryption (AES-256)
    - Password (scrypt)
    - Biometrics (Android Keystore)
  - Screen capture prevention
  - Tap to reveal ability
- Multiple ways to add new entries
  - Scan QR code
  - Enter details manually
  - Import from files
    - andOTP
    - FreeOTP
    - Aegis
  - Import from apps (requires root):
    - Google Authenticator
    - Steam
- Supported algorithms:
   - HOTP ([RFC 4226](https://tools.ietf.org/html/rfc4226))
   - TOTP ([RFC 6238](https://tools.ietf.org/html/rfc6238))
   - Steam ([RFC 6238](https://tools.ietf.org/html/rfc6238) with custom
     encoding)
- Compatible with Google Authenticator
- Organization
  - Custom or default icons
  - Drag and drop
  - Custom groups
  - Advanced entry editing
- Material design with multiple themes:
  - Light theme
  - Dark theme
  - Amoled / true dark theme
- Export (plaintext or encrypted)

## Screenshots

[<img width=200 alt="Screenshot 1"
src="metadata/en-US/images/phoneScreenshots/screenshot1.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot1.png?raw=true)
[<img width=200 alt="Screenshot 2"
src="metadata/en-US/images/phoneScreenshots/screenshot2.png?raw=true">](/metadata/en-US/images/phoneScreenshots/screenshot2.png?raw=true)
[<img width=200 alt="Screenshot 3"
src="metadata/en-US/images/phoneScreenshots/screenshot3.png?raw=true">](/metadata/en-US/images/phoneScreenshots/screenshot3.png?raw=true)

[<img width=200 alt="Screenshot 4"
src="metadata/en-US/images/phoneScreenshots/screenshot4.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot4.png?raw=true)
[<img width=200 alt="Screenshot 5"
src="metadata/en-US/images/phoneScreenshots/screenshot5.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot5.png?raw=true)
[<img width=200 alt="Screenshot 6"
src="metadata/en-US/images/phoneScreenshots/screenshot6.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot6.png?raw=true)

[<img width=200 alt="Screenshot 7"
src="metadata/en-US/images/phoneScreenshots/screenshot7.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot7.png?raw=true)

## Downloads

Aegis is available on the Google Play Store and on F-Droid.

[<img height=80 alt="Get it on Google Play"
src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
/>](http://play.google.com/store/apps/details?id=com.beemdevelopment.aegis)
[<img height="80" alt="Get it on F-Droid"
src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
/>](https://f-droid.org/app/com.beemdevelopment.aegis)
      
### Verification

APK releases on Google Play and GitHub are signed using the same key. They can
be verified using
[apksigner](https://developer.android.com/studio/command-line/apksigner.html#options-verify):

```
apksigner verify --print-certs --verbose aegis.apk
```

The output should look like:

```
Verifies
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
```

The certificate fingerprints should correspond to the ones listed below:

```
Owner: CN=Beem Development
Issuer: CN=Beem Development
Serial number: 172380c
Valid from: Sat Feb 09 14:05:49 CET 2019 until: Wed Feb 03 14:05:49 CET 2044
Certificate fingerprints:
   MD5:  AA:EE:86:DB:C7:B8:88:9F:1F:C9:D0:7A:EC:37:36:32
   SHA1: 59:FB:63:B7:1F:CE:95:74:6C:EB:1E:1A:CB:2C:2E:45:E5:FF:13:50
   SHA256: C6:DB:80:A8:E1:4E:52:30:C1:DE:84:15:EF:82:0D:13:DC:90:1D:8F:E3:3C:F3:AC:B5:7B:68:62:D8:58:A8:23
```

### Community

- [aegis-icons](https://github.com/krisu5/aegis-icons)

  While we're still working on better icon support in Aegis, krisu5 has started
  a third-party repository for icons that you may want to check out.

  [<img width=500 alt="Aegis-icons preview"
  src="https://raw.githubusercontent.com/krisu5/aegis-icons/master/showcase.png">](https://github.com/krisu5/aegis-icons)

## Libraries

- [TextDrawable](https://github.com/amulyakhare/TextDrawable) by Amulya Khare
- [FloatingActionButton](https://github.com/Clans/FloatingActionButton) by
  Dmytro Tarianyk
- [AppIntro](https://github.com/AppIntro/AppIntro) by Paolo Rotolo
- [Krop](https://github.com/avito-tech/krop) by Avito Technology
- [SpongyCastle](https://github.com/rtyley/spongycastle) by Roberto Tyley
- [CircleImageView](https://github.com/hdodenhof/CircleImageView) by Henning
  Dodenhof
- [barcodescanner](https://github.com/dm77/barcodescanner) by Dushyanth
- [libsu](https://github.com/topjohnwu/libsu) by John Wu

## License

This project is licensed under the GNU General Public License v3.0. See the
[LICENSE](LICENSE) file for details.
