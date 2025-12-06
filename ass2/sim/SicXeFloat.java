/**
 * Utility for encoding and decoding SIC/XE 48-bit floating point numbers.
 */
final class SicXeFloat {
    private static final int FRACTION_BITS = 40;
    private static final long FRACTION_MASK = (1L << FRACTION_BITS) - 1L;
    private static final int EXPONENT_BITS = 7;
    private static final int EXPONENT_BIAS = 64;
    private static final int EXPONENT_MAX = (1 << EXPONENT_BITS) - 1;
    private static final long SIGN_MASK = 1L << 47;

    private SicXeFloat() {
    }

    public static double fromRaw(long raw) {
        raw &= 0xFFFFFFFFFFFFL;
        if (raw == 0) {
            return 0.0;
        }
        long sign = (raw & SIGN_MASK) != 0 ? -1L : 1L;
        int exponent = (int) ((raw >> FRACTION_BITS) & EXPONENT_MAX);
        long fraction = raw & FRACTION_MASK;
        if (exponent == 0 && fraction == 0) {
            return 0.0;
        }
        if (exponent == 0) {
            return 0.0;
        }
        double mantissa = 1.0 + (fraction / (double) (1L << FRACTION_BITS));
        int power = exponent - EXPONENT_BIAS;
        double value = Math.scalb(mantissa, power);
        return sign < 0 ? -value : value;
    }

    public static long toRaw(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Cannot encode NaN or infinity in SIC/XE format.");
        }
        if (value == 0.0) {
            return 0L;
        }
        long signBit = value < 0 ? SIGN_MASK : 0L;
        double absValue = Math.abs(value);

        int exponent = 0;
        double normalized = absValue;
        while (normalized >= 2.0) {
            normalized /= 2.0;
            exponent++;
        }
        while (normalized < 1.0) {
            normalized *= 2.0;
            exponent--;
            if (exponent < -EXPONENT_BIAS + 1) {
                return 0L;
            }
        }

        int storedExponent = exponent + EXPONENT_BIAS;
        if (storedExponent <= 0) {
            return 0L;
        }
        if (storedExponent >= EXPONENT_MAX) {
            storedExponent = EXPONENT_MAX;
            normalized = Math.nextDown(2.0);
        }

        double fractionPart = normalized - 1.0;
        long fraction = Math.round(fractionPart * (1L << FRACTION_BITS));
        if (fraction == (1L << FRACTION_BITS)) {
            fraction = 0;
            storedExponent++;
            if (storedExponent >= EXPONENT_MAX) {
                storedExponent = EXPONENT_MAX;
            }
        }

        long raw = signBit | ((long) storedExponent << FRACTION_BITS) | (fraction & FRACTION_MASK);
        return raw & 0xFFFFFFFFFFFFL;
    }
}
