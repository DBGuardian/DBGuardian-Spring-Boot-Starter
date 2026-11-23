package io.dbguardian.dialect.mariadb;

import io.dbguardian.spi.GtidDatabaseDialect.PositionOrder;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MariaDbGtidPosition {

    private static final Pattern GTID = Pattern.compile("([0-9]{1,10})-([0-9]{1,10})-([0-9]{1,20})");
    private static final long MAX_UINT32 = 0xffffffffL;

    private MariaDbGtidPosition() {
    }

    static PositionOrder compare(String left, String right) {
        Map<Long, MariaDbGtidPosition.Entry> leftDomains = parse(left);
        Map<Long, MariaDbGtidPosition.Entry> rightDomains = parse(right);
        if (leftDomains == null || rightDomains == null) {
            return PositionOrder.UNKNOWN;
        }
        boolean ahead = false;
        boolean behind = false;
        for (Map.Entry<Long, Entry> domain : leftDomains.entrySet()) {
            Entry l = domain.getValue();
            Entry r = rightDomains.get(domain.getKey());
            if (r == null) {
                ahead = true;
                continue;
            }
            // A domain keeps advancing across promotions; the originating server may change.
            int sequenceOrder = Long.compareUnsigned(l.sequence, r.sequence);
            if (sequenceOrder == 0 && l.server != r.server) {
                return PositionOrder.DIVERGED;
            }
            ahead |= sequenceOrder > 0;
            behind |= sequenceOrder < 0;
        }
        behind |= !leftDomains.keySet().containsAll(rightDomains.keySet());
        if (ahead && behind) {
            return PositionOrder.DIVERGED;
        }
        return ahead ? PositionOrder.AHEAD : behind ? PositionOrder.BEHIND : PositionOrder.EQUAL;
    }

    private static Map<Long, Entry> parse(String position) {
        if (position == null) {
            return null;
        }
        Map<Long, Entry> domains = new HashMap<Long, Entry>();
        if (position.trim().isEmpty()) {
            return domains;
        }
        for (String token : position.split(",", -1)) {
            Matcher matcher = GTID.matcher(token.trim());
            if (!matcher.matches()) {
                return null;
            }
            long domain = Long.parseLong(matcher.group(1));
            long server = Long.parseLong(matcher.group(2));
            if (domain > MAX_UINT32 || server > MAX_UINT32) {
                return null;
            }
            try {
                long sequence = Long.parseUnsignedLong(matcher.group(3));
                if (domains.put(Long.valueOf(domain), new Entry(server, sequence)) != null) {
                    return null;
                }
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return domains;
    }

    private static final class Entry {
        private final long server;
        private final long sequence;

        private Entry(long server, long sequence) {
            this.server = server;
            this.sequence = sequence;
        }
    }
}
