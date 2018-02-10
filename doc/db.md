# Database

The database is encoded in a simple binary format with JSON content at its core.

## Encryption

The content of the database can be encrypted with AES in GCM mode. The nonce and
authentication tag are stored in the plain section of this file.

If there is no Slots and/or EncryptionParameters section in the file, it is
implied that the content is unencrypted and Aegis will try to parse it as such.

## Format

The file format starts with a small header that contains some magic and a
version number. A list of sections follows. These sections contain some
information needed to perform decryption of the database. The (encrypted)
content of the database starts after the end marker section.

All integers are encoded in Little Endian.

### Header

| Length | Contents                 |
|:-------|:-------------------------|
| `5`    | "AEGIS" encoded in ASCII |
| `1`    | `uint8_t` Version        |
| `?`    | List of sections         |
| `?`    | Content                  |

### Sections

| Length | Contents          |
|:-------|:------------------|
| `1`    | `uint8_t` ID      |
| `4`    | `uint32_t` Length |
| `?`    | Section data      |

ID can be one of:

| Value  | Name                 |
|:-------|:---------------------|
| `0x00` | EncryptionParameters |
| `0x01` | Slots                |
| `0xFF` | End marker           |

#### EncryptionParameters

| Length | Contents |
|:-------|:---------|
| `12`   | Nonce    |
| `16`   | Tag      |

#### Slots

This section contains a SHA-256 hash of the master key and a list of slots. The
hash is used to verify the integrity of a decrypted slot. Note that this is
meant for convenience, not as a security measure.

| Length | Contents                  |
|:-------|:--------------------------|
| `32`   | `uint8_t` Master Key Hash |
| `?`    | Slots                     |

All slots contain the master key encrypted with raw AES. The key that is used for
encryption depends on the slot type.

A slot has the following structure.

| Length | Contents            |
|:-------|:--------------------|
| `1`    | `uint8_t` Type      |
| `16`   | ID                  |
| `32`   | Encrypted key       |
| `?`    | Additional data     |

Type can be one of:

| Value  | Name        |
|:-------|:------------|
| `0x00` | Raw         |
| `0x01` | Password    |
| `0x02` | Fingerprint |

##### Raw

This slot type contains no additional data.

##### Password

With this slot type the key used for the master key encryption is derived from a
user-provided password. The key derivation function is scrypt. The parameters
used for scrypt are stored as additional data.

| Length | Contents     |
|:-------|:-------------|
| `4`    | `uint32_t` N |
| `4`    | `uint32_t` r |
| `4`    | `uint32_t` p |
| `32`   | Salt         |

##### Fingerprint

A fingerprint slot is exactly the same as a Raw slot.

#### End marker

This section indicates the end of the list of sections. This section doesn't
have any content and thus its length is 0.

### Content

The content of the database is a JSON file encoded in UTF-8.

```json
{
    "version": 1,
    "counter": 10,
    "entries":
    [
        {
            "id": 1,
            "name": "ACME Co/john@example.com",
            "url": "otpauth://totp/ACME%20Co:john@example.com?secret=HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ&issuer=ACME%20Co&algorithm=SHA1&digits=6&period=30",
        },
        ...
    ]
}
```
