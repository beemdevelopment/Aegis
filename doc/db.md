# Database

The database is encoded in a simple binary format with JSON content at its
core.

## Encryption

The content of the database can be encrypted with AES in GCM mode. The nonce
and authentication tag are stored in the plain section of this file. The
storage place for the key depends on the security level that is used. This will
be discussed later.

## Format

The file format starts with a small header that contains some magic, the
version number and the level of security. A list of sections follows. These
sections contain some information needed to perform decryption of the database.
The (encrypted) content of the database starts after the end marker section.

All integers are encoded in Little Endian.

### Header

| Length | Contents                 |
|:-------|:-------------------------|
| `5`    | "AEGIS" encoded in ASCII |
| `1`    | `uint8_t` Version        |
| `1`    | `uint8_t` Level          |
| `?`    | List of sections         |
| `?`    | Content                  |

#### Levels

As mentioned above, there are different levels of security that a user can
choose from. No encryption, encryption using a derived key and encryption using
a key that's stored in the Android KeyStore.

| Value  | Name     |
|:-------|:---------|
| `0x00` | None     |
| `0x01` | Derived  |
| `0x02` | KeyStore |

The 'KeyStore' level expects an EncryptionParameters section. The 'Derived'
level expects an EncryptionParameters section **and** a DerivationParameters section.
The 'None' level expects no additional sections.

##### None

No encryption at all. The content of the database is stored in plain text.

##### Derived

If this level is used, the key is derived from a user-provided password using
PBKDF2 with SHA256 as the underlying PRF. The parameters used for PBKDF2 (salt,
number of iterations) are stored in the plain section of this file. The key is
not stored anywhere.

##### KeyStore

The key is kept in the Android keystore and can optionally be set up to require
user authentication (fingerprint). This security level is only available on
Android M and above.

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
| `0x01` | DerivationParameters |
| `0xFF` | End marker           |

#### EncryptionParameters

| Length | Contents |
|:-------|:---------|
| `12`   | Nonce    |
| `16`   | Tag      |

#### DerivationParameters

| Length | Contents                        |
|:-------|:--------------------------------|
| `8`    | `uint64_t` Number of iterations |
| `32`   | Salt                            |

#### End marker

This section indicates the end of the list of sections. This section doesn't
have any content and thus its length is 0.

### Content

The content of the database is a JSON file encoded in UTF-8. As mentioned
above, it's encrypted.

``` json
{
    "version": 1,
    "entries":
    [
        {
            "id": 1,
            "name": "ACME Co/john@example.com",
            "url": "otpauth://totp/ACME%20Co:john@example.com?secret=HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ&issuer=ACME%20Co&algorithm=SHA1&digits=6&period=30",
            "order": 0,
        },
        ...
    ]
}
```
