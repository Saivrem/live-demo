#!/bin/sh
set -eu
mkdir -p /run/fail2ban /var/lib/fail2ban
# Fail visibly if NET_ADMIN or the host's nftables support is missing.
if ! nft list tables >/dev/null; then
    echo 'Fail2ban requires Linux, host networking, NET_ADMIN and nftables kernel support.' >&2
    exit 1
fi
# Apache opens this file at startup; wait briefly when the shared volume is new.
for attempt in $(seq 1 30); do
    [ ! -f /var/log/apache2/access.log ] || break
    sleep 1
done
exec "$@"
