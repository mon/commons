package com.timepath.io;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.logging.Logger;

public class BitBuffer {

    private static final Logger LOG = Logger.getLogger(BitBuffer.class.getName());
    /**
     * Total number of bits
     */
    private final int        capacityBits;
    private final ByteBuffer source;
    /**
     * Internal field holding the current byte in the source buffer
     */
    private       short      b;
    /**
     * Position in bits
     */
    private       int        position;
    /**
     * Stores bit access offset
     */
    private       int        positionBit;
    /**
     * Internal field holding the remaining bits in the current byte
     */
    private       int        remainingBits;

    public BitBuffer(ByteBuffer bytes) {
        source = bytes; capacityBits = source.capacity() * 8;
    }

    public static void main(String... args) {
        BitBuffer scramble = new BitBuffer(ByteBuffer.wrap(new byte[] {
                (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE
        })); int shift = 1; scramble.position(0, shift); int first = scramble.get(); scramble.position(0, shift);
        int second = scramble.get(); assert first == second; LOG.info(first + " vs " + second); int number = 1;
        String expected = Long.toBinaryString(number); int bitLength = expected.length();
        for(int i = 0; i < ( 32 - bitLength ); i++) {
            int n = number << i; ByteBuffer b = ByteBuffer.allocate(4); b.order(ByteOrder.LITTLE_ENDIAN); b.putInt(n); b.flip();
            BitBuffer bb = new BitBuffer(b); bb.getBits(i); long bits = bb.getBits(bitLength);
            LOG.info(Long.toBinaryString(bits) + " == " + expected + " ?");
        } number = (int) ( Math.random() * Integer.MAX_VALUE ); ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.LITTLE_ENDIAN); b.putInt(number); b.flip(); BitBuffer bb = new BitBuffer(b);
        String s1 = Integer.toBinaryString(number); String s2 = ""; for(int i = 0; i < s1.length(); i++) {
            s2 = bb.getBits(1) + s2;
        } LOG.info(s1); LOG.info(s2);
    }

    public int capacity() {
        return capacityBits / 8;
    }

    byte get() {
        return getByte();
    }

    public void get(byte... dst) {
        get(dst, 0, dst.length);
    }

    public void get(byte[] dst, int offset, int length) {
        for(int i = offset; i < ( offset + length ); i++) {
            dst[i] = get();
        }
    }

    public boolean getBoolean() {
        return getBits(1) != 0;
    }

    public long getBits(int n) {
        long data = 0; for(int i = 0; i < n; i++) {
            if(remainingBits == 0) {
                nextByte();
            } remainingBits--; int m = 1 << ( positionBit++ % 8 ); if(( b & m ) != 0) {
                data |= 1 << i;
            }
        } position += n; return data;
    }

    /**
     * Loads source data into internal byte
     */
    private void nextByte() {
        b = (short) ( source.get() & 0xFF ); remainingBits = 8;
    }

    public byte getByte() {
        return (byte) getBits(8);
    }

    public double getDouble() {
        return Double.longBitsToDouble(getLong());
    }

    long getLong() {
        return getBits(64);
    }

    public float getFloat() {
        return Float.intBitsToFloat(getInt());
    }

    public int getInt() {
        return (int) getBits(32);
    }

    public short getShort() {
        return (short) getBits(16);
    }

    @SuppressWarnings("empty-statement")
    String getString(int limit) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(); for(byte c; ( c = getByte() ) != 0; baos.write(c)) ;
        if(limit > 0) {
            get(new byte[limit - baos.size()]);
        } return Charset.forName("UTF-8").decode(ByteBuffer.wrap(baos.toByteArray())).toString();
    }

    public String getString() {
        return getString(0);
    }

    /**
     * @return True if more than 1 byte is available
     */
    public boolean hasRemaining() {
        return remaining() > 0;
    }

    /**
     * @return Remaining bytes
     */
    public int remaining() {
        return remainingBits() / 8;
    }

    /**
     * @return Remaining bits
     */
    public int remainingBits() {
        return capacityBits - position;
    }

    /**
     * @return True if more than 1 bit is available
     */
    public boolean hasRemainingBits() {
        return remainingBits() > 0;
    }

    /**
     * @return The limit in bytes
     */
    public int limit() {
        return capacityBits / 8;
    }

    public void order(ByteOrder bo) {
    }

    /**
     * Set the position
     *
     * @param newPosition
     */
    public void position(int newPosition) {
        position(newPosition, 0);
    }

    /**
     * Set the position
     *
     * @param newPosition
     *         Byte offset
     * @param bits
     *         Bit offset
     */
    public void position(int newPosition, int bits) {
        source.position(newPosition); position = newPosition * 8; positionBit = bits; remainingBits = 0;
    }

    /**
     * @return Position in bytes
     */
    public int position() {
        return positionBits() / 8;
    }

    /**
     * @return Position in bits
     */
    public int positionBits() {
        return position;
    }
}