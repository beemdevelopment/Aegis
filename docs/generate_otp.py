#!/usr/bin/env python3

# example usage: ./scripts/decrypt.py --input ./app/src/test/resources/com/beemdevelopment/aegis/importers/aegis_encrypted.json --entryname Mason
# password: test

import argparse
import getpass
import json
import sys

import pyotp

import decrypt


def main():
    parser = argparse.ArgumentParser(description="Decrypt an Aegis vault and generate an OTP code")
    parser.add_argument("--input", dest="input", required=True, help="encrypted Aegis vault file")
    parser.add_argument("--entryname", dest="entryname", required=True,
                        help="name of the entry for which you want to generate the OTP code")
    args = parser.parse_args()

    password = getpass.getpass().encode("utf-8")

    db = decrypt.decrypt_db(args.input, password)

    entries = json.loads(db)
    entries_found = []

    for entry in entries['entries']:
        name = entry.get('name', '')

        # Looks also for substrings
        if args.entryname.lower() in name.lower():
            entries_found.append(entry)

    for entry in entries_found:
        if entry.get('type', '') == 'totp':
            totp = pyotp.TOTP(entry['info']['secret'], interval=entry['info']['period'])
            print("Entry %s - issuer %s - TOTP generated: %s" % (
                entry.get('name', ''), entry.get('issuer', ''), totp.now()))
        else:
            print("OTP type not supported: %s" % entry.get('type', ''))
            sys.exit(2)


if __name__ == '__main__':
    main()
