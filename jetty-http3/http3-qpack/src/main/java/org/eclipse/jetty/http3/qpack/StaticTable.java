package org.eclipse.jetty.http3.qpack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.util.Index;

public class StaticTable
{
    private static final String EMPTY = "";
    public static final String[][] STATIC_TABLE =
        {
            {null, null},
            /* 1  */ {":authority", EMPTY},
            /* 2  */ {":method", "GET"},
            /* 3  */ {":method", "POST"},
            /* 4  */ {":path", "/"},
            /* 5  */ {":path", "/index.html"},
            /* 6  */ {":scheme", "http"},
            /* 7  */ {":scheme", "https"},
            /* 8  */ {":status", "200"},
            /* 9  */ {":status", "204"},
            /* 10 */ {":status", "206"},
            /* 11 */ {":status", "304"},
            /* 12 */ {":status", "400"},
            /* 13 */ {":status", "404"},
            /* 14 */ {":status", "500"},
            /* 15 */ {"accept-charset", EMPTY},
            /* 16 */ {"accept-encoding", "gzip, deflate"},
            /* 17 */ {"accept-language", EMPTY},
            /* 18 */ {"accept-ranges", EMPTY},
            /* 19 */ {"accept", EMPTY},
            /* 20 */ {"access-control-allow-origin", EMPTY},
            /* 21 */ {"age", EMPTY},
            /* 22 */ {"allow", EMPTY},
            /* 23 */ {"authorization", EMPTY},
            /* 24 */ {"cache-control", EMPTY},
            /* 25 */ {"content-disposition", EMPTY},
            /* 26 */ {"content-encoding", EMPTY},
            /* 27 */ {"content-language", EMPTY},
            /* 28 */ {"content-length", EMPTY},
            /* 29 */ {"content-location", EMPTY},
            /* 30 */ {"content-range", EMPTY},
            /* 31 */ {"content-type", EMPTY},
            /* 32 */ {"cookie", EMPTY},
            /* 33 */ {"date", EMPTY},
            /* 34 */ {"etag", EMPTY},
            /* 35 */ {"expect", EMPTY},
            /* 36 */ {"expires", EMPTY},
            /* 37 */ {"from", EMPTY},
            /* 38 */ {"host", EMPTY},
            /* 39 */ {"if-match", EMPTY},
            /* 40 */ {"if-modified-since", EMPTY},
            /* 41 */ {"if-none-match", EMPTY},
            /* 42 */ {"if-range", EMPTY},
            /* 43 */ {"if-unmodified-since", EMPTY},
            /* 44 */ {"last-modified", EMPTY},
            /* 45 */ {"link", EMPTY},
            /* 46 */ {"location", EMPTY},
            /* 47 */ {"max-forwards", EMPTY},
            /* 48 */ {"proxy-authenticate", EMPTY},
            /* 49 */ {"proxy-authorization", EMPTY},
            /* 50 */ {"range", EMPTY},
            /* 51 */ {"referer", EMPTY},
            /* 52 */ {"refresh", EMPTY},
            /* 53 */ {"retry-after", EMPTY},
            /* 54 */ {"server", EMPTY},
            /* 55 */ {"set-cookie", EMPTY},
            /* 56 */ {"strict-transport-security", EMPTY},
            /* 57 */ {"transfer-encoding", EMPTY},
            /* 58 */ {"user-agent", EMPTY},
            /* 59 */ {"vary", EMPTY},
            /* 60 */ {"via", EMPTY},
            /* 61 */ {"www-authenticate", EMPTY}
        };

    public static final int STATIC_SIZE = STATIC_TABLE.length - 1;

    private final Map<HttpField, QpackContext.Entry> __staticFieldMap = new HashMap<>();
    private final Index<QpackContext.StaticEntry> __staticNameMap;
    private final QpackContext.StaticEntry[] __staticTableByHeader = new QpackContext.StaticEntry[HttpHeader.values().length];
    private final QpackContext.StaticEntry[] __staticTable = new QpackContext.StaticEntry[STATIC_TABLE.length];

    public StaticTable()
    {
        Index.Builder<QpackContext.StaticEntry> staticNameMapBuilder = new Index.Builder<QpackContext.StaticEntry>().caseSensitive(false);
        Set<String> added = new HashSet<>();
        for (int i = 1; i < STATIC_TABLE.length; i++)
        {
            QpackContext.StaticEntry entry = null;

            String name = STATIC_TABLE[i][0];
            String value = STATIC_TABLE[i][1];
            HttpHeader header = HttpHeader.CACHE.get(name);
            if (header != null && value != null)
            {
                switch (header)
                {
                    case C_METHOD:
                    {

                        HttpMethod method = HttpMethod.CACHE.get(value);
                        if (method != null)
                            entry = new QpackContext.StaticEntry(i, new StaticTableHttpField(header, name, value, method));
                        break;
                    }

                    case C_SCHEME:
                    {

                        HttpScheme scheme = HttpScheme.CACHE.get(value);
                        if (scheme != null)
                            entry = new QpackContext.StaticEntry(i, new StaticTableHttpField(header, name, value, scheme));
                        break;
                    }

                    case C_STATUS:
                    {
                        entry = new QpackContext.StaticEntry(i, new StaticTableHttpField(header, name, value, value));
                        break;
                    }

                    default:
                        break;
                }
            }

            if (entry == null)
                entry = new QpackContext.StaticEntry(i, header == null ? new HttpField(STATIC_TABLE[i][0], value) : new HttpField(header, name, value));

            __staticTable[i] = entry;

            if (entry._field.getValue() != null)
                __staticFieldMap.put(entry._field, entry);

            if (!added.contains(entry._field.getName()))
            {
                added.add(entry._field.getName());
                staticNameMapBuilder.with(entry._field.getName(), entry);
            }
        }
        __staticNameMap = staticNameMapBuilder.build();

        for (HttpHeader h : HttpHeader.values())
        {
            QpackContext.StaticEntry entry = __staticNameMap.get(h.asString());
            if (entry != null)
                __staticTableByHeader[h.ordinal()] = entry;
        }
    }

    public QpackContext.Entry get(HttpField field)
    {
        return __staticFieldMap.get(field);
    }

    public QpackContext.Entry get(String name)
    {
        return __staticNameMap.get(name);
    }

    public QpackContext.Entry get(int index)
    {
        if (index >= __staticTable.length)
            return null;
        return __staticTable[index];
    }

    public QpackContext.Entry get(HttpHeader header)
    {
        int index = header.ordinal();
        if (index >= __staticTableByHeader.length)
            return null;
        return __staticTableByHeader[index];
    }
}