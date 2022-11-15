#!/bin/sh

if [ $API -lt 29 ]; then
  abort "! Neo-Wellbeing require Android 10+ to work properly"
fi
