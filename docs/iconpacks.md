# Icon packs

### The format

Icon packs are .ZIP archives with a collection of icons and a ``pack.json``
file. The icon pack definition is a JSON file, formatted like the example below.
All icon packs have a name, a UUID, a version and a list of icons. The version
number is incremented when a new version of the icon pack is released. The UUID
is randomly generated once and stays the same across different versions.

```json
{
    "uuid": "c553f06f-2a17-46ca-87f5-56af90dd0500",
    "name": "Alex' Icon Pack",
    "version": 1,
    "icons": [
        {
            "name": "Google",
            "filename": "services/Google.png",
            "category": "Services",
            "issuer": [ "google" ]
        },
        {
            "name": "Blizzard",
            "filename": "services/Blizzard.png",
            "category": "Gaming",
            "issuer": [ "blizzard", "battle.net" ]
        }
    ]
}
```

Every icon definition contains the filename of the icon file, relative to the
root of the .ZIP archive. Icon definitions also have a list of strings that the
Issuer field in Aegis is matched against for automatic selection of an icon for
new entries. Matching is done in a case-insensitive manner. There's also a
category field. Optionally, icons can also have a name.

The following image formats are supported, in order of preference:

| Name | MIME          | Extension |
|:-----|:--------------|:----------|
| SVG  | image/svg+xml | .svg      |
| PNG  | image/png     | .png      |
| JPEG | image/jpeg    | .jpg      |

Any files in the .ZIP archive that are not the ``pack.json`` file or referred to
in the icons list are ignored. Such files are not extracted when importing the
icon pack into Aegis.

### Using icon packs in Aegis

Users can download an icon pack from the internet and import it into Aegis
through the settings menu. Aegis extracts the icon pack to
``icons/{uuid}/{version}``, relative to its internal storage directory. So for
the example icon pack above, that'd be:
``icons/c553f06f-2a17-46ca-87f5-56af90dd0500/1``. If it has an old version of
the icon pack, it will be removed after successful extraction of the newer
version. 

After that, Aegis will start proposing icons for new entries if the issuer
matches with one of the icons in the pack. We'll also have an icon selection
dialog, where all of the icons in the pack appear. When the user selects an
icon, it is copied and stored in the vault file.
