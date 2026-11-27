#!/usr/bin/env python3
"""Prepare ignored signing files from Actions secrets; never print secret values."""
import base64
import binascii
import os
from pathlib import Path
import sys


def escape(value):
    # java.util.Properties loads ISO-8859-1 and interprets escapes.
    result = ''
    for char in value:
        if char in '\\ :=#!':
            result += '\\' + char
        elif char == '\n':
            result += '\\n'
        elif char == '\r':
            result += '\\r'
        elif char == '\t':
            result += '\\t'
        elif ord(char) < 32 or ord(char) > 126:
            result += ''.join('\\u%04x' % int.from_bytes(unit, 'big')
                              for unit in [char.encode('utf-16-be')[i:i+2]
                                           for i in range(0, len(char.encode('utf-16-be')), 2)])
        else:
            result += char
    return result


def main():
    names = ('KEYSTORE_BASE64', 'KEYSTORE_PASSWORD', 'KEY_ALIAS', 'KEY_PASSWORD')
    missing = [name for name in names if not os.environ.get(name)]
    if missing:
        sys.exit('::error::Missing required signing secrets: ' + ', '.join(missing))
    try:
        keystore = base64.b64decode(''.join(os.environ['KEYSTORE_BASE64'].split()), validate=True)
    except (ValueError, binascii.Error):
        sys.exit('::error::KEYSTORE_BASE64 is not valid base64')
    if not keystore:
        sys.exit('::error::Decoded keystore is empty')
    os.umask(0o077)
    Path('app/release.keystore').write_bytes(keystore)
    properties = {
        'storeFile': 'release.keystore',
        'storePassword': os.environ['KEYSTORE_PASSWORD'],
        'keyAlias': os.environ['KEY_ALIAS'],
        'keyPassword': os.environ['KEY_PASSWORD'],
    }
    Path('key.properties').write_text(''.join(f'{key}={escape(value)}\n' for key, value in properties.items()), encoding='ascii')
    print('All four signing secrets are present; signing files prepared (values not logged).')


if __name__ == '__main__':
    main()
