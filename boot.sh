#!/usr/bin/env bash
export BOOT_JVM_OPTIONS="$BOOT_JVM_OPTIONS -Xmx2g -XX:MaxPermSize=128m -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -client -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xverify:none"
boot "$@"