

### I can no longer use biometrics to unlock the app. What should I do?

If you could previously unlock Aegis with biometrics, but suddenly can't do so
anymore, this is probably caused by a change made to the security settings of
your device. The app will tell you when this happened in most cases. To resolve
this, unlock the app with your password, disable biometric unlock in the
settings of Aegis and re-enable it.

### Why does Aegis keep prompting me for my password, even though I have enabled biometric authentication?

You're probably encountering the password reminder. Try entering your password
to unlock the vault once. After that, Aegis will prompt for biometrics by
default again until it's time for another password reminder.

Since forgetting your password will result in loss of access to the contents of
the vault, __we do NOT recommend disabling the password reminder__.

### Aegis uses SHA1 for most/all of my tokens. Isn't that insecure?

The hash algorithm is imposed by the service you're setting up 2FA for (e.g.
Google, Facebook, GitHub, etc). There is nothing we can do about that. If we
were to change this on Aegis' end, the tokens would stop working. Furthermore,
when using SHA1 in an HMAC calculation, the currently known issues in SHA1 are
not of concern.

### Why doesn't Aegis support biometric unlock for my device, even though it works with other apps?

The reason for this is pretty technical. In short, since you're not entering
your password when using biometric unlock, Aegis needs some other way to decrypt
the vault. For this purpose, we generate and use a key in the Android Keystore,
telling it to only allow us to use that key if the user authenticates using
their biometrics first. Some devices have buggy implementations of this feature,
resulting in the error displayed to you by Aegis in an error dialog.

If biometrics works with other apps, but not with Aegis, that means those other
apps probably perform a weaker form of biometric authentication.

## Backups

### How can I back up my Aegis vault to the cloud automatically?

Aegis can only automatically back up to the cloud if the app of your cloud
provider is installed on your device and fully participates in the Android
Storage Access Framework. Aegis doesn't have access to the internet and we don't
have plans to change this, so adding support for specific cloud providers in the
app is not possible.

Cloud providers currently known to be supported:
- Nextcloud

Another common setup is to configure Aegis to back up to a folder on local
storage of your device and then have a separate app (like
[Syncthing](https://syncthing.net/)) sync that folder anywhere you want.

## Encrypted Backups

### Why do I not get prompted to enter an encryption password when exporting?

Aegis uses the same password you have configured to encrypt your vault as the
password which is used when exporting and importing your vault; so when prompted, 
you will enter that when importing your vault.

## Importing

### When importing from Authenticator Plus, an error is shown claiming that Accounts.txt is missing

Make sure you supply an Authenticator Plus export file obtained through
__Settings -> Backup & Restore -> Export as Text and HTML__. The ``.db`` format
is not supported.

If it still doesn't work, please report the issue to us. As a temporary
workaround, you can try extracting the ZIP archive on a computer, recreating it
without a password and then importing that into Aegis. Another option is
extracting the ZIP archive on a computer and importing the resulting
Accounts.txt file into Aegis with the "Plain text" import option.
