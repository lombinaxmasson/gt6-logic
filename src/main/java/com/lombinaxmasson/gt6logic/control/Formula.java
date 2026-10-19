package com.lombinaxmasson.gt6logic.control;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.lombinaxmasson.gt6logic.signal.SignalLevels;

/**
 * One line, in signal levels. {@code a} is the front face and {@code b} is the back.
 * {@code 1} is one level ({@code UNIT}). Multiplication and division stay in that unit.
 */
public final class Formula {
    public static final int MAX_LENGTH = 64;
    /** Levels inside a line. One level is 10000, so {@code 0.5} is exact. The wire converts at the boundary. */
    private static final long SCALE = 10_000L;
    /** First entry is the default. Right click walks this list. */
    public static final String[] PRESETS = {
            "clamp(a * 0.5 + b, 0, 15)",
            "min(a, b)",
            "max(a - b, 0)",
            "clamp(a * 2, 0, 15)"
    };

    public record Value(boolean ok, long signal) {
        public static Value bad() {
            return new Value(false, 0L);
        }

        public static Value of(long signal) {
            return new Value(true, signal);
        }
    }

    private Formula() {}

    public static int nextPreset(String current) {
        for (int i = 0; i < PRESETS.length; i++) {
            if (PRESETS[i].equals(current)) {
                return (i + 1) % PRESETS.length;
            }
        }
        return 0;
    }

    public static Value evaluate(String source, long a, long b) {
        if (source == null || source.length() > MAX_LENGTH || source.isBlank()) {
            return Value.bad();
        }
        try {
            Parser parser = new Parser(source, fromWire(a), fromWire(b));
            long value = parser.parseExpr();
            parser.skip();
            if (parser.index != parser.text.length()) {
                return Value.bad();
            }
            return Value.of(toWire(value));
        } catch (FormulaException | ArithmeticException ex) {
            return Value.bad();
        }
    }

    private static final class FormulaException extends RuntimeException {
        private FormulaException() {}
    }

    private static final class Parser {
        private final String text;
        private final long a;
        private final long b;
        private int index;
        private int depth;

        private Parser(String text, long a, long b) {
            this.text = text;
            this.a = a;
            this.b = b;
        }

        private long parseExpr() {
            enter();
            try {
                long left = parseTerm();
                while (true) {
                    skip();
                    if (index >= text.length()) {
                        return left;
                    }
                    char operator = text.charAt(index);
                    if (operator != '+' && operator != '-') {
                        return left;
                    }
                    index++;
                    long right = parseTerm();
                    left = operator == '+' ? add(left, right) : sub(left, right);
                }
            } finally {
                depth--;
            }
        }

        private long parseTerm() {
            enter();
            try {
                long left = parseUnary();
                while (true) {
                    skip();
                    if (index >= text.length()) {
                        return left;
                    }
                    char operator = text.charAt(index);
                    if (operator != '*' && operator != '/') {
                        return left;
                    }
                    index++;
                    long right = parseUnary();
                    left = operator == '*' ? mul(left, right) : div(left, right);
                }
            } finally {
                depth--;
            }
        }

        private long parseUnary() {
            enter();
            try {
                skip();
                if (index < text.length() && text.charAt(index) == '-') {
                    index++;
                    return sub(0L, parseUnary());
                }
                return parsePrimary();
            } finally {
                depth--;
            }
        }

        private long parsePrimary() {
            enter();
            try {
                return readPrimary();
            } finally {
                depth--;
            }
        }

        private long readPrimary() {
            skip();
            if (index >= text.length()) {
                throw new FormulaException();
            }
            char c = text.charAt(index);
            if (c == '(') {
                index++;
                long value = parseExpr();
                skip();
                expect(')');
                return value;
            }
            if (isDigit(c) || c == '.') {
                return number();
            }
            if (isLetter(c)) {
                String name = word();
                skip();
                if (index < text.length() && text.charAt(index) == '(') {
                    return call(name);
                }
                return variable(name);
            }
            throw new FormulaException();
        }

        private long call(String name) {
            expect('(');
            long[] args = arguments();
            return switch (name) {
                case "clamp" -> {
                    if (args.length != 3) {
                        throw new FormulaException();
                    }
                    yield clamp(args[0], args[1], args[2]);
                }
                case "min" -> two(args, false);
                case "max" -> two(args, true);
                default -> throw new FormulaException();
            };
        }

        private static long two(long[] args, boolean max) {
            if (args.length != 2) {
                throw new FormulaException();
            }
            return max ? Math.max(args[0], args[1]) : Math.min(args[0], args[1]);
        }

        private long[] arguments() {
            skip();
            if (index < text.length() && text.charAt(index) == ')') {
                index++;
                return new long[0];
            }
            long[] values = new long[4];
            int count = 0;
            while (true) {
                if (count == values.length) {
                    throw new FormulaException();
                }
                values[count++] = parseExpr();
                skip();
                if (index >= text.length()) {
                    throw new FormulaException();
                }
                char c = text.charAt(index);
                if (c == ',') {
                    index++;
                    continue;
                }
                if (c == ')') {
                    index++;
                    long[] exact = new long[count];
                    System.arraycopy(values, 0, exact, 0, count);
                    return exact;
                }
                throw new FormulaException();
            }
        }

        private long variable(String name) {
            return switch (name) {
                case "a" -> a;
                case "b" -> b;
                default -> throw new FormulaException();
            };
        }

        private long number() {
            int start = index;
            boolean dot = false;
            long numerator = 0L;
            long denominator = 1L;
            while (index < text.length()) {
                char c = text.charAt(index);
                if (isDigit(c)) {
                    numerator = Math.addExact(Math.multiplyExact(numerator, 10L), c - '0');
                    if (dot) {
                        denominator = Math.multiplyExact(denominator, 10L);
                    }
                    index++;
                } else if (c == '.' && !dot) {
                    dot = true;
                    index++;
                } else {
                    break;
                }
            }
            if (index == start || (dot && denominator == 1L)) {
                throw new FormulaException();
            }
            return SignalLevels.multiplyDivide(SCALE, numerator, denominator);
        }

        private String word() {
            int start = index;
            while (index < text.length() && isLetter(text.charAt(index))) {
                index++;
            }
            if (index == start) {
                throw new FormulaException();
            }
            return text.substring(start, index);
        }

        private void expect(char c) {
            skip();
            if (index >= text.length() || text.charAt(index) != c) {
                throw new FormulaException();
            }
            index++;
        }

        private void skip() {
            while (index < text.length() && text.charAt(index) <= ' ') {
                index++;
            }
        }

        private void enter() {
            if (++depth > 32) {
                throw new FormulaException();
            }
        }

        private static boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        private static boolean isLetter(char c) {
            return c >= 'a' && c <= 'z';
        }
    }

    private static long add(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException ex) {
            return right > 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
    }

    private static long sub(long left, long right) {
        try {
            return Math.subtractExact(left, right);
        } catch (ArithmeticException ex) {
            return right > 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
    }

    private static long mul(long left, long right) {
        if (left == 0L || right == 0L) {
            return 0L;
        }
        boolean negative = (left < 0L) ^ (right < 0L);
        long magnitude = SignalLevels.multiplyDivide(abs(left), abs(right), SCALE);
        return finish(negative, magnitude);
    }

    private static long div(long left, long right) {
        if (right == 0L) {
            return 0L;
        }
        boolean negative = (left < 0L) ^ (right < 0L);
        long magnitude = SignalLevels.multiplyDivide(abs(left), SCALE, abs(right));
        return finish(negative, magnitude);
    }

    private static long clamp(long value, long low, long high) {
        long lo = Math.min(low, high);
        long hi = Math.max(low, high);
        if (value < lo) {
            return lo;
        }
        return Math.min(value, hi);
    }

    private static long finish(boolean negative, long magnitude) {
        if (!negative) {
            return magnitude;
        }
        return magnitude == Long.MAX_VALUE ? Long.MIN_VALUE : -magnitude;
    }

    private static long fromWire(long signal) {
        return SignalLevels.multiplyDivide(GtSignalCarrier.clamp(signal), SCALE, GtSignalCarrier.UNIT);
    }

    /** Signed. The block clamps this onto the wire, so a negative line stays visible to tests. */
    private static long toWire(long levels) {
        if (levels == 0L) {
            return 0L;
        }
        boolean negative = levels < 0L;
        long magnitude = SignalLevels.multiplyDivide(abs(levels), GtSignalCarrier.UNIT, SCALE);
        if (!negative) {
            return magnitude;
        }
        return magnitude == 0L ? 0L : -magnitude;
    }

    private static long abs(long value) {
        return value == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(value);
    }
}
