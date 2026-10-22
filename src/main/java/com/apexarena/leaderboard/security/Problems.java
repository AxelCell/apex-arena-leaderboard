package com.apexarena.leaderboard.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Filters run before Spring MVC, so @RestControllerAdvice cannot format their errors.
 * This writes the same problem+json shape by hand.
 */
final class Problems {

    private Problems() {
    }

    static void write(HttpServletResponse res, int status, String title, String detail, String path)
            throws IOException {
        res.setStatus(status);
        res.setContentType("application/problem+json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + esc(title) + "\",\"status\":" + status
                + ",\"detail\":\"" + esc(detail) + "\",\"instance\":\"" + esc(path) + "\"}");
    }

    private static String esc(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                default -> {
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
                }
            }
        }
        return b.toString();
    }
}
