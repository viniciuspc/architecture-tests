# AD2 - Encryption

### Status
#### Proposed
#### Accepted
#### Rejected
#### Deprecated
#### Superseded

## Status

Accepted

## Context

The Security requirement for this system demands that data shouldn't be accessed by unauthorized parties (Confidentiality), leading to the need of keeping that data encrypted while it is stored.

## Decision

Encrypt data using AES 128 algorythm.
AES 128 is the "less secure" encryption method comparing to the other flavors of AES. Although it still provides a secure enought encryption result for the data being stored. (AES is demmed secure until +- 2030).

https://www.keylength.com/en/3/

## Consequences

AES128 has less impact in performance rather than AES192/AES256. Hoever, the level of security provided isn't as stronger as the one provided by the second algorythms.

## Alternatives
AES192; AES256; DES;