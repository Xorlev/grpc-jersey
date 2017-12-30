package com.fullcontact.rpc.jersey.util;

import com.google.protobuf.util.JsonFormat;

public final class JsonUtil {
    public static final JsonFormat.Printer PRINTER = JsonFormat.printer().includingDefaultValueFields();
    public static final JsonFormat.Printer PRINTER_WITHOUT_WHITESPACE = JsonFormat.printer()
                                                                .includingDefaultValueFields()
                                                                .omittingInsignificantWhitespace();

    private JsonUtil() {}
}
