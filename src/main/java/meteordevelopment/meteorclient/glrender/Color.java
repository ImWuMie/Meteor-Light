package meteordevelopment.meteorclient.glrender;

import javax.annotation.Nonnull;

public class Color {
    public static final int TRANSPARENT = 0;

    /**
     * Return the alpha component of a color int. This is the same as saying
     * color >>> 24
     */
    public static int alpha(int color) {
        return color >>> 24;
    }

    /**
     * Return the red component of a color int. This is the same as saying
     * (color >> 16) & 0xFF
     */
    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    /**
     * Return the green component of a color int. This is the same as saying
     * (color >> 8) & 0xFF
     */
    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    /**
     * Return the blue component of a color int. This is the same as saying
     * color & 0xFF
     */
    public static int blue(int color) {
        return color & 0xFF;
    }


    public static int rgb(int red, int green, int blue) {
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }


    public static int rgb(float red, float green, float blue) {
        return 0xFF000000 |
                ((int) (red * 255.0f + 0.5f) << 16) |
                ((int) (green * 255.0f + 0.5f) << 8) |
                (int) (blue * 255.0f + 0.5f);
    }


    public static int argb(int alpha, int red, int green, int blue) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }


    public static int argb(float alpha, float red, float green, float blue) {
        return ((int) (alpha * 255.0f + 0.5f) << 24) |
                ((int) (red * 255.0f + 0.5f) << 16) |
                ((int) (green * 255.0f + 0.5f) << 8) |
                (int) (blue * 255.0f + 0.5f);
    }


    public static int parseColor(@Nonnull String colorString) {
        if (colorString.charAt(0) == '#') {
            int color = Integer.parseUnsignedInt(colorString.substring(1), 16);
            if (colorString.length() == 7) {
                // Set the alpha value
                color |= 0xFF000000;
            } else if (colorString.length() != 9) {
                throw new IllegalArgumentException("Unknown color: " + colorString);
            }
            return color;
        } else if (colorString.startsWith("0x")) { // do not support upper case
            int color = Integer.parseUnsignedInt(colorString.substring(2), 16);
            if (colorString.length() == 8) {
                // Set the alpha value
                color |= 0xFF000000;
            } else if (colorString.length() != 10) {
                throw new IllegalArgumentException("Unknown color: " + colorString);
            }
            return color;
        }
        throw new IllegalArgumentException("Unknown color prefix: " + colorString);
    }


    public static int blend(@Nonnull BlendMode mode, int src, int dst) {
        return switch (mode) {
            case CLEAR -> TRANSPARENT;
            case SRC -> src;
            case DST -> dst;
            case SRC_OVER -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0xFF)
                    yield src;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0)
                    yield src;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float srcR = red(src) / 255.0f * srcA;
                float srcG = green(src) / 255.0f * srcA;
                float srcB = blue(src) / 255.0f * srcA;
                float dstA = dstAlpha / 255.0f;
                float dstR = red(dst) / 255.0f * dstA;
                float dstG = green(dst) / 255.0f * dstA;
                float dstB = blue(dst) / 255.0f * dstA;
                // blend
                float oneMinusSrcA = 1.0f - srcA;
                float outA = srcA + oneMinusSrcA * dstA;
                if (outA == 0.0f)
                    yield TRANSPARENT;
                float outR = srcR + oneMinusSrcA * dstR;
                float outG = srcG + oneMinusSrcA * dstG;
                float outB = srcB + oneMinusSrcA * dstB;
                // un-premultiply the out color
                float invA = 1.0f / outA;
                yield argb(outA, outR * invA, outG * invA, outB * invA);
            }
            case DST_OVER -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0)
                    yield dst;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0xFF)
                    yield dst;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float srcR = red(src) / 255.0f * srcA;
                float srcG = green(src) / 255.0f * srcA;
                float srcB = blue(src) / 255.0f * srcA;
                float dstA = dstAlpha / 255.0f;
                float dstR = red(dst) / 255.0f * dstA;
                float dstG = green(dst) / 255.0f * dstA;
                float dstB = blue(dst) / 255.0f * dstA;
                // blend
                float oneMinusDstA = 1.0f - dstA;
                float outA = dstA + oneMinusDstA * srcA;
                if (outA == 0.0f)
                    yield TRANSPARENT;
                float outR = dstR + oneMinusDstA * srcR;
                float outG = dstG + oneMinusDstA * srcG;
                float outB = dstB + oneMinusDstA * srcB;
                // un-premultiply the out color
                float invA = 1.0f / outA;
                yield argb(outA, outR * invA, outG * invA, outB * invA);
            }
            case SRC_IN -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0)
                    yield TRANSPARENT;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0xFF)
                    yield src;
                if (dstAlpha == 0)
                    yield TRANSPARENT;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float srcR = red(src) / 255.0f * srcA;
                float srcG = green(src) / 255.0f * srcA;
                float srcB = blue(src) / 255.0f * srcA;
                float dstA = dstAlpha / 255.0f;
                // blend
                float outA = srcA * dstA;
                if (outA == 0.0f)
                    yield TRANSPARENT;
                float outR = srcR * dstA;
                float outG = srcG * dstA;
                float outB = srcB * dstA;
                // un-premultiply the out color
                float invA = 1.0f / outA;
                yield argb(outA, outR * invA, outG * invA, outB * invA);
            }
            case DST_IN -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0xFF)
                    yield dst;
                if (srcAlpha == 0)
                    yield TRANSPARENT;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0)
                    yield TRANSPARENT;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float dstA = dstAlpha / 255.0f;
                float dstR = red(dst) / 255.0f * dstA;
                float dstG = green(dst) / 255.0f * dstA;
                float dstB = blue(dst) / 255.0f * dstA;
                // blend
                float outA = dstA * srcA;
                if (outA == 0.0f)
                    yield TRANSPARENT;
                float outR = dstR * srcA;
                float outG = dstG * srcA;
                float outB = dstB * srcA;
                // un-premultiply the out color
                float invA = 1.0f / outA;
                yield argb(outA, outR * invA, outG * invA, outB * invA);
            }
            case SRC_OUT -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0)
                    yield TRANSPARENT;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0)
                    yield src;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float srcR = red(src) / 255.0f * srcA;
                float srcG = green(src) / 255.0f * srcA;
                float srcB = blue(src) / 255.0f * srcA;
                float dstA = dstAlpha / 255.0f;
                // blend
                float oneMinusDstA = 1.0f - dstA;
                float outA = srcA * oneMinusDstA;
                if (outA == 0.0f)
                    yield TRANSPARENT;
                float outR = srcR * oneMinusDstA;
                float outG = srcG * oneMinusDstA;
                float outB = srcB * oneMinusDstA;
                // un-premultiply the out color
                float invA = 1.0f / outA;
                yield argb(outA, outR * invA, outG * invA, outB * invA);
            }
            case DST_OUT -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0)
                    yield dst;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0)
                    yield TRANSPARENT;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float dstA = dstAlpha / 255.0f;
                float dstR = red(dst) / 255.0f * dstA;
                float dstG = green(dst) / 255.0f * dstA;
                float dstB = blue(dst) / 255.0f * dstA;
                // blend
                float oneMinusSrcA = 1.0f - srcA;
                float outA = srcA * oneMinusSrcA;
                if (outA == 0.0f)
                    yield TRANSPARENT;
                float outR = dstR * oneMinusSrcA;
                float outG = dstG * oneMinusSrcA;
                float outB = dstB * oneMinusSrcA;
                // un-premultiply the out color
                float invA = 1.0f / outA;
                yield argb(outA, outR * invA, outG * invA, outB * invA);
            }
            case SRC_ATOP -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0)
                    yield dst;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0)
                    yield TRANSPARENT;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float srcR = red(src) / 255.0f * srcA;
                float srcG = green(src) / 255.0f * srcA;
                float srcB = blue(src) / 255.0f * srcA;
                float dstA = dstAlpha / 255.0f;
                float dstR = red(dst) / 255.0f * dstA;
                float dstG = green(dst) / 255.0f * dstA;
                float dstB = blue(dst) / 255.0f * dstA;
                // blend
                float oneMinusSrcA = 1.0f - srcA;
                float outR = srcR * dstA + dstR * oneMinusSrcA;
                float outG = srcG * dstA + dstG * oneMinusSrcA;
                float outB = srcB * dstA + dstB * oneMinusSrcA;
                // un-premultiply the out color
                float invA = 1.0f / dstA;
                yield argb(dstA, outR * invA, outG * invA, outB * invA);
            }
            case DST_ATOP -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0)
                    yield TRANSPARENT;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0)
                    yield src;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float srcR = red(src) / 255.0f * srcA;
                float srcG = green(src) / 255.0f * srcA;
                float srcB = blue(src) / 255.0f * srcA;
                float dstA = dstAlpha / 255.0f;
                float dstR = red(dst) / 255.0f * dstA;
                float dstG = green(dst) / 255.0f * dstA;
                float dstB = blue(dst) / 255.0f * dstA;
                // blend
                float oneMinusDstA = 1.0f - dstA;
                float outR = dstR * srcA + srcR * oneMinusDstA;
                float outG = dstG * srcA + srcG * oneMinusDstA;
                float outB = dstB * srcA + srcB * oneMinusDstA;
                // un-premultiply the out color
                float invA = 1.0f / srcA;
                yield argb(srcA, outR * invA, outG * invA, outB * invA);
            }
            case XOR -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0)
                    yield dst;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0)
                    yield src;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float srcR = red(src) / 255.0f * srcA;
                float srcG = green(src) / 255.0f * srcA;
                float srcB = blue(src) / 255.0f * srcA;
                float dstA = dstAlpha / 255.0f;
                float dstR = red(dst) / 255.0f * dstA;
                float dstG = green(dst) / 255.0f * dstA;
                float dstB = blue(dst) / 255.0f * dstA;
                // blend
                float oneMinusSrcA = 1.0f - srcA;
                float oneMinusDstA = 1.0f - dstA;
                float outA = srcA * oneMinusDstA + dstA * oneMinusSrcA;
                if (outA == 0.0f)
                    yield TRANSPARENT;
                float outR = srcR * oneMinusDstA + dstR * oneMinusSrcA;
                float outG = srcG * oneMinusDstA + dstG * oneMinusSrcA;
                float outB = srcB * oneMinusDstA + dstB * oneMinusSrcA;
                // un-premultiply the out color
                float invA = 1.0f / outA;
                yield argb(outA, outR * invA, outG * invA, outB * invA);
            }
            case PLUS -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0)
                    yield dst;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0)
                    yield src;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float srcR = red(src) / 255.0f * srcA;
                float srcG = green(src) / 255.0f * srcA;
                float srcB = blue(src) / 255.0f * srcA;
                float dstA = dstAlpha / 255.0f;
                float dstR = red(dst) / 255.0f * dstA;
                float dstG = green(dst) / 255.0f * dstA;
                float dstB = blue(dst) / 255.0f * dstA;
                // blend
                float outA = Math.min(srcA + dstA, 1);
                if (outA == 0.0f)
                    yield TRANSPARENT;
                float outR = Math.min(srcR + dstR, 1);
                float outG = Math.min(srcG + dstG, 1);
                float outB = Math.min(srcB + dstB, 1);
                // un-premultiply the out color
                float invA = 1.0f / outA;
                yield argb(outA, outR * invA, outG * invA, outB * invA);
            }
            case MULTIPLY -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0)
                    yield TRANSPARENT;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0)
                    yield TRANSPARENT;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float srcR = red(src) / 255.0f * srcA;
                float srcG = green(src) / 255.0f * srcA;
                float srcB = blue(src) / 255.0f * srcA;
                float dstA = dstAlpha / 255.0f;
                float dstR = red(dst) / 255.0f * dstA;
                float dstG = green(dst) / 255.0f * dstA;
                float dstB = blue(dst) / 255.0f * dstA;
                // blend
                float outA = srcA * dstA;
                if (outA == 0.0f)
                    yield TRANSPARENT;
                float outR = srcR * dstR;
                float outG = srcG * dstG;
                float outB = srcB * dstB;
                // un-premultiply the out color
                float invA = 1.0f / outA;
                yield argb(outA, outR * invA, outG * invA, outB * invA);
            }
            case SCREEN -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0)
                    yield dst;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0)
                    yield src;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float srcR = red(src) / 255.0f * srcA;
                float srcG = green(src) / 255.0f * srcA;
                float srcB = blue(src) / 255.0f * srcA;
                float dstA = dstAlpha / 255.0f;
                float dstR = red(dst) / 255.0f * dstA;
                float dstG = green(dst) / 255.0f * dstA;
                float dstB = blue(dst) / 255.0f * dstA;
                // blend
                float outA = srcA + dstA - srcA * dstA;
                if (outA == 0.0f)
                    yield TRANSPARENT;
                float outR = srcR + dstR - srcR * dstR;
                float outG = srcG + dstG - srcG * dstG;
                float outB = srcB + dstB - srcB * dstB;
                // un-premultiply the out color
                float invA = 1.0f / outA;
                yield argb(outA, outR * invA, outG * invA, outB * invA);
            }
            default -> src;
        };
    }
}
