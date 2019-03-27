# Aegis

Aegis is a free, secure and open source 2FA app for Android.

# Features

- Free and open source
- Secure
  - Encryption (AES-256)
    - Password (scrypt)
	- Fingerprint (Android Keystore)
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
- Supported algorithms:
   - HOTP ([RFC 4226](https://tools.ietf.org/html/rfc4226))
   - TOTP ([RFC 6238](https://tools.ietf.org/html/rfc6238))
- Compatible with Google Authenticator
- Organization
  - Custom or default icons
  - Drag and drop
  - Custom groups
  - Advanced entry editing
- Material design
  - Light and dark themes
- Export (plaintext or encrypted)

## Screenshots

[<img width=200 alt="Main Activity" src="metadata/en-US/images/phoneScreenshots/screenshot_main.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot_main.png?raw=true)
[<img width=200 alt="Settings Activity" src="metadata/en-US/images/phoneScreenshots/screenshot_settings.png?raw=true">](/metadata/en-US/images/phoneScreenshots/screenshot_settings.png?raw=true)
[<img width=200 alt="Edit Activity" src="metadata/en-US/images/phoneScreenshots/screenshot_edit.png?raw=true">](/metadata/en-US/images/phoneScreenshots/screenshot_edit.png?raw=true)

[<img width=200 alt="Main Activity" src="metadata/en-US/images/phoneScreenshots/screenshot_main_group.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot_main_group.png?raw=true)
[<img width=200 alt="Main Activity" src="metadata/en-US/images/phoneScreenshots/screenshot_main_dark.png?raw=true">](metadata/en-US/images/phoneScreenshots/screenshot_main_dark.png?raw=true)

## Downloads

Aegis is available in the Google Play Store.

[<img height=80 alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" />](http://play.google.com/store/apps/details?id=com.beemdevelopment.aegis)

There are plans to make it available on F-Droid as well.

## Libraries

- [TextDrawable](https://github.com/amulyakhare/TextDrawable) by Amulya Khare
- [FloatingActionButton](https://github.com/Clans/FloatingActionButton) by Dmytro Tarianyk
- [AppIntro](https://github.com/AppIntro/AppIntro) by Paolo Rotolo
- [Krop](https://github.com/avito-tech/krop) by Avito Technology
- [SpongyCastle](https://github.com/rtyley/spongycastle) by Roberto Tyley
- [Swirl](https://github.com/mattprecious/swirl) by Matthew Precious
- [CircleImageView](https://github.com/hdodenhof/CircleImageView) by Henning Dodenhof
- [barcodescanner](https://github.com/dm77/barcodescanner) by Dushyanth
- [libsu](https://github.com/topjohnwu/libsu) by John Wu

## License

This project is licensed under the GNU General Public License v3.0. See the [LICENSE](LICENSE) file for details.
