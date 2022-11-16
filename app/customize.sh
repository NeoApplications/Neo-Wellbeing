#!/bin/sh

if [ "$API" -lt 29 ]; then
  abort "! Neo Wellbeing requires Android 10 or later"
fi
