package com.uniwise.common.ansi;

public class HttpMethodColor {
    public static final String RESET = "\u001B[0m";

    public static String color(String method) {
        return switch (method) {
            case "GET" -> "\u001B[34m";     // Blue
            case "POST" -> "\u001B[32m";    // Green
            case "PUT" -> "\u001B[33m";     // Yellow
            case "PATCH" -> "\u001B[38;5;208m"; // Orange
            case "DELETE" -> "\u001B[31m";  // Red
            case "OPTIONS" -> "\u001B[35m"; // Purple
            case "HEAD" -> "\u001B[37m";    // Gray
            default -> RESET;
        };
    }

    public static String statusColor(int status) {
        if (status >= 200 && status < 300) return "\u001B[32m"; // Green
        if (status >= 300 && status < 400) return "\u001B[34m"; // Blue
        if (status >= 400 && status < 500) return "\u001B[33m"; // Yellow
        if (status >= 500) return "\u001B[31m"; // Red
        return RESET;
    }
}
