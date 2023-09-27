#!/bin/bash

set -x
set -e

if [[ -z "$KYBER_DIR"  ]]; then
    echo "set KYBER_DIR to the location of Kyber checked out from https://github.com/itzmeanjan/kyber/tree/master"
    exit 1
fi

KYBER_HEADERS=$KYBER_DIR/include
SHA3_HEADERS=$KYBER_DIR/sha3/include

g++ -std=c++20 -Wall -O3 -march=native -I $KYBER_HEADERS -I $SHA3_HEADERS encdec.cpp -o ../security/encdec
