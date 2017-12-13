#!/bin/bash
if [ "$1" = "" ]; then
    echo "Usage: ${0##*/} <k8s-port>"
    exit 2
fi

curl http://192.168.99.100:$1/jbreak-coffee/resources/orders -XPOST -i -H 'Content-Type: application/json' -d '{"origin":"Colombia","type":"espresso"}'
