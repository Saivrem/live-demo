FROM debian:trixie-slim
RUN apt-get update && apt-get install -y --no-install-recommends \
        fail2ban nftables tini ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && rm -f /etc/fail2ban/jail.d/defaults-debian.conf
COPY jail.local fail2ban.local /etc/fail2ban/
COPY filter.d/ /etc/fail2ban/filter.d/
COPY entrypoint.sh /usr/local/bin/fail2ban-entrypoint
COPY tests/ /opt/fail2ban-tests/
RUN mkdir -p /var/log/apache2 && touch /var/log/apache2/access.log
RUN chmod +x /usr/local/bin/fail2ban-entrypoint \
    && python3 /opt/fail2ban-tests/check_filters.py \
    && fail2ban-client -t
ENTRYPOINT ["/usr/bin/tini", "-g", "--", "/usr/local/bin/fail2ban-entrypoint"]
CMD ["fail2ban-server", "-f", "-x", "start"]
