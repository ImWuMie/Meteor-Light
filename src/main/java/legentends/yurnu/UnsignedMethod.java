// By yurnu
// Can better use OpenGL
package legentends.yurnu;

public class UnsignedMethod {
    // Unsigned char (short to char)
    static public short add(short a, short b) {
        return (short) ((unsigned_char(a) + unsigned_char(b)) % 256);
    }

    static public short unsigned_char(short a) {
        return (short) (((byte) a + 256) % 256);
    }

    // Unsigned short (int to short)
    static public int add(int a, int b) {
        return (unsigned_short(a) + unsigned_short(b)) % 65536;
    }
    static public int unsigned_short(int a) {
        return ((short)a + 65536) % 65536;
    }


    // Unsigned long or int
    static public long add(long a, long b) {
        return (unsigned_long(a) + unsigned_long(b)) % 4294967296L;
    }

    static public long unsigned_long(long a) {
        return ((int)a + 4294967296L) % 4294967296L;
    }
}
