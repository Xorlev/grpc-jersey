package com.fullcontact.rpc.jersey;

import com.google.protobuf.util.JsonFormat;

/**
 * Holder for JsonFormat printers. Allows JVM-global overrides of printer.
 */
public final class JsonHandler {
    private static JsonFormat.Printer UNARY = JsonFormat.printer().includingDefaultValueFields();
    private static JsonFormat.Printer STREAM = JsonFormat.printer()
                                                                .includingDefaultValueFields()
                                                                .omittingInsignificantWhitespace();

    private JsonHandler() {}

    public static JsonFormat.Printer unaryPrinter() {
        return UNARY;
    }

    public static JsonFormat.Printer streamPrinter() {
        return STREAM;
    }

    /**
     * Sets the {@link com.google.protobuf.util.JsonFormat.Printer} used by unary RPC calls.
     *
     * <p>This method should only be called during initialization.
     */
    public static void setUnaryPrinter(JsonFormat.Printer printer) {
        JsonHandler.UNARY = printer;
    }

    /**
     * Sets the {@link com.google.protobuf.util.JsonFormat.Printer} used by streaming RPC calls.
     *
     * <p>This method should only be called during initialization.
     */
    public static void setStreamPrinter(JsonFormat.Printer printer) {
        JsonHandler.STREAM = printer;
    }
}
