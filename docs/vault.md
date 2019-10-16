# Aegis Vault

Aegis persists the user's tokens to a file. This file is referred to as the
__vault__. Users can configure the app to store the vault in plain text or to
encrypt it with a password.

This document describes Aegis' security design and file format. It's split up
into two parts. First, the cryptographic primitives and use of them for
encryption are discussed. The second section documents the details of the file
format of the vault.

## Security

### Primitives

Two cryptographic primitives were selected for use in Aegis. An Authenticated
Encryption with Associated Data (AEAD) cipher and a Key Derivation Function
(KDF).

#### AEAD

__AES-256__ in __GCM__ mode is used as the AEAD cipher to ensure the
confidentility, integrity and authenticity of the vault contents.

It requires a unique 96-bit nonce for each invocation with the same key.
However, it is not possible to use a monotically increasing counter for this in
this case, because a future use case could involve using the vault on multiple
devices simultaneously, which would almost certainly result in nonce reuse. This
is suboptimal, because 96 bits is not large enough to comfortably generate an
unlimited amount of random numbers without getting collisions at some point
either. As a repeat of the nonce would have catastrophic consequences for the
confidentiality and integrity of the ciphertext, NIST strongly recommends not
exceeding 2<sup>32</sup> invocations when using random nonces with GCM. As such,
the security of the Aegis vault also relies on the assumption that this limit is
never exceeded. In the case of Aegis, this is a reasonable assumption to make,
as it's highly unlikely that a user will ever come close to saving the vault
2<sup>32</sup> times.

_Switching to a nonce misuse-resistant cipher like AES-GCM-SIV or a cipher with
a larger (192 bits) nonce like XChaCha-Poly1305 will be explored in the future._

#### KDF

__scrypt__ is used as the KDF to derive a key from a user-provided password,
with the following parameters:

| Parameter |  Value         |
| :-------- | :------------- |
| N         | 2<sup>15</sup> |
| r         | 8              |
| p         | 1              |

These are the same parameters as Android itself uses to derive a key for
full-disk encryption. Because of the memory limitations Android apps have, it's
not possible to increase them without running into OOM conditions on most
devices.

_Argon2 is a more modern KDF that provides an advantage over scrypt because it
allows tweaking the memory-hardness parameter and CPU-hardness parameter
separately, whereas scrypt ties those together into one cost parameter (N). As
many applications have started using Argon2 in production, it seems that it has
withstood the test of time. It will be considered as an alternative option to
switch to in the future._

### Encryption

When a vault is first created, a random 256-bit key is generated that is used to
encrypt the contents with AES in GCM mode. This key is referred to as the
__master key__.

Aegis supports unlocking a vault with multiple different credentials. The main
credential is a key derived from a user-provided password. In addition to that,
users can also add a key backed by the Android KeyStore (authorized by biometrics)
as a credential.

#### Slots

Each credential that should be able to encrypt/decrypt the contents of a vault
has its own __slot__. Every slot contains a copy of the master key that is
encrypted with its credential. The process of encrypting a key with another key
is known as __key wrapping__. This allows obtaining the master key by providing
any of the credentials. An important consequence is that the master key is only
as secure as the weakest credential.

This design is similar to and largely inspired by LUKS' key slot system.

#### Integrity

Because of the use of an AEAD for encryption, the vault contents and encrypted
master keys in the slots are checked for integrity and authenticity. The rest of
the file is not.

### Overview

An attempt was made to create a clear overview of the encryption system.

![](diagram.svg)

## Format

The vault is stored in JSON and encoded in UTF-8. The upper-level structure is
shown below:

```json
{
    "version": 1,
    "header": {},
    "db": {}
}
```

It starts with a ``version`` number and a ``header``. If a backwards
incompatible change is introduced to the content format, the version number will
be incremented. The vault contents are stored under ``db``. Its value depends on
wheter the vault is encrypted or not. If it is, the value is a string containing
the Base64 encoded (with padding) ciphertext of the vault contents. Otherwise,
the value is a JSON object.

Full examples of a plain text vault and an encrypted vault are available in the
[testdata](/testdata) folder. There's also a Python script that can decrypt an
Aegis vault given the password: [scripts/decrypt.py](/scripts/decrypt.py).

### Header

The header starts with the list of ``slots``. It also has a ``params`` object
that holds the ``nonce`` and ``tag`` that were produced during encryption,
encoded as a hexadecimal string. 

Setting ``slots`` and ``params`` to null indicates that the vault is not
encrypted and Aegis will try to parse it as such.

```json
{
    "slots": [],
    "params": {
        "nonce": "0123456789abcdef01234567",
        "tag": "0123456789abcdef0123456789abcdef"
    }
}
```

#### Slots

The different slot types are identified with a numerical ID. 

| Type        | ID   |
| :---------- | :--- |
| Raw         | 0x00 |
| Biometric   | 0x01 |
| Password    | 0x02 |

##### Raw

This slot type is used for raw AES key credentials. It is not used directly in
the app, but all other slots are based on this slot type, so this section
applies to all of them.

Each slot transforms its credential in a way that it can be used to encrypt the
master key with AES-256 in GCM mode. The ``nonce``, ``tag`` and encrypted
``key`` are encoded as a hexadecimal string and stored together. Slots also have
a unique randomly generated ``UUID`` (version 4).

```json
{
    "type": 0,
    "uuid": "01234567-89ab-cdef-0123-456789abcdef",
    "key": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
    "key_params": {
        "nonce": "0123456789abcdef01234567",
        "tag": "0123456789abcdef0123456789abcdef"
    }
}
```

##### Biometric

The structure of the Biometric slot is exactly the same as the Raw slot. The
difference is that the wrapper key is backed by the Android KeyStore, whereas
Raw slots don't imply use of a particular storage type.

##### Password

As noted earlier, scrypt is used to derive a 256-bit key from a user-provided
password. A random 256-bit ``salt`` is generated and passed to scrypt to protect
against rainbow table attacks. Its stored along with the ``N``, ``r`` and ``p``
parameters.

```json
{
    "type": 1,
    "uuid": "01234567-89ab-cdef-0123-456789abcdef",
    "key": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
    "key_params": {
        "nonce": "0123456789abcdef01234567",
        "tag": "0123456789abcdef0123456789abcdef"
    },
    "n": 32768,
    "r": 8,
    "p": 1,
    "salt": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
}
```

### Content

The content is a JSON object encoded in UTF-8.

```json
{
    "version": 1,
    "entries": []
}
```

It has a ``version`` number and a list of ``entries``. If a backwards
incompatible change is introduced to the content format, the version number will
be incremented.

#### Entries

Each entry has a unique randomly generated ``UUID`` (version 4), as well as a
``name`` and ``issuer`` to idenfity the account name and service that the token
is for. Entries can also have an icon. These are JPEG's encoded in Base64 with
padding. The ``info`` object holds information specific to the OTP type. The
``secret`` is encoded in Base32 without padding.

There are a number of supported types:

| Type                | ID      |
| :------------------ | :------ |
| TOTP  | "totp"  |
| HOTP  | "hotp"  |
| Steam               | "steam" |

There is no specification available for Steam's OTP algorithm. It's essentially
the same as TOTP, but it uses a different final encoding step. Aegis's
implementation of it can be found in
[crypto/otp/OTP.java](https://github.com/beemdevelopment/Aegis/blob/master/app/src/main/java/com/beemdevelopment/aegis/crypto/otp/OTP.java).

The following algorithms are supported for all OTP types:

| Algorithm | ID       |
| :-------- | :------- |
| SHA-1     | "SHA1"   |
| SHA-256   | "SHA256" |
| SHA-512   | "SHA512" |

Example of a TOTP entry:

```json
{
    "type": "totp",
    "uuid": "01234567-89ab-cdef-0123-456789abcdef",
    "name": "Bob",
    "issuer": "Google",
    "icon": null,
    "info": {
        "secret": "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567",
        "algo": "SHA1",
        "digits": 6,
        "period": 30
    }
}
```
