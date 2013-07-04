package com.timepath;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class DataUtils {

    /**
     * Conversion table:
     * C Java
     * byte 8 8
     * char 8 16
     * short 16 16
     * int 16 32
     * long 32 64
     * float 32 32
     */
    private static final Logger LOG = Logger.getLogger(DataUtils.class.getName());

    /**
     * Defensive copy
     *
     * @param source
     *
     * @return
     */
    public static ByteBuffer getSlice(ByteBuffer source) {
        return getSlice(source, source.remaining());
    }

    /**
     * Reads length bytes ahead, turns them into a ByteBuffer, and then jumps there
     *
     * @param source
     * @param length
     *
     * @return
     */
    public static ByteBuffer getSlice(ByteBuffer source, int length) {
        int originalLimit = source.limit();
        source.limit(source.position() + length);
        ByteBuffer sub = source.slice();
        source.position(source.limit());
        source.limit(originalLimit);
        sub.order(ByteOrder.LITTLE_ENDIAN);
        return sub;
    }

    public static ByteBuffer getSafeSlice(ByteBuffer source, int length) {
        if(length > source.remaining()) {
            length = source.remaining();
        }
        return getSlice(source, length);
    }

    public static ByteBuffer mapFile(File f) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(f, "r");
//        FileInputStream fis = new FileInputStream(f);
        FileChannel fc = raf.getChannel();
        MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, f.length());
        mbb.order(ByteOrder.LITTLE_ENDIAN);
        return mbb;
    }
    
    public static String readEntire(InputStream in, String encoding) {
        Scanner s = new Scanner(in, encoding);
        s.useDelimiter("\\A");
        String str = s.hasNext() ? s.next() : "";
        return str;
    }
    
    public static String readEntire(InputStream in) {
        return readEntire(in, "UTF-8");
    }

    public static ByteBuffer mapInputStream(InputStream is) throws IOException {
        int bs = 8192;
        if(!(is instanceof BufferedInputStream)) {
//            is = new BufferedInputStream(is);
            bs = is.available();
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[bs];
        while((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();

        ByteBuffer mbb = ByteBuffer.wrap(buffer.toByteArray());
        mbb.order(ByteOrder.LITTLE_ENDIAN);
        return mbb;
    }

    public static String getString(ByteBuffer source) {
        ByteBuffer[] cloned = DataUtils.getTextBuffer(source.duplicate(), true);
        source.position(source.position() + (cloned[0].limit() - cloned[0].position()));
        return Charset.forName("UTF-8").decode(cloned[1]).toString();
    }

    public static String getText(ByteBuffer source) {
        return getText(source, false);
    }

    public static String getText(ByteBuffer source, boolean terminator) {
        return Charset.forName("UTF-8").decode(getTextBuffer(source, terminator)[1]).toString();
    }

    public static ByteBuffer[] getTextBuffer(ByteBuffer source, boolean terminatorCheck) {
        int originalPosition = source.position();
        int originalLimit = source.limit();
        int inclusiveEnd = source.limit();
        int trimmedEnd = source.limit();
        if(terminatorCheck) {
            while(source.remaining() > 0) {
                if(source.get() == 0x00) { // Check for null terminator
                    inclusiveEnd = source.position();
                    trimmedEnd = source.position() - 1;
                    break;
                }
            }
        }
        source.position(originalPosition);
        source.limit(inclusiveEnd);
        ByteBuffer inclusive = source.slice();
        source.limit(trimmedEnd);
        ByteBuffer trimmed = source.slice();
        source.limit(originalLimit);

        return new ByteBuffer[] {inclusive, trimmed};
    }
    
    public static String readZeroString(ByteBuffer buf) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for(;;) {
            byte b = buf.get();
            if(b == 0) {
                break;
            }
            baos.write(b);
        }
//        LOG.info(Arrays.toString(baos.toByteArray()) + "");
        return Charset.forName("UTF-8").decode(ByteBuffer.wrap(baos.toByteArray())).toString();
    }

    //<editor-fold defaultstate="collapsed" desc="Old stuff">
    public static byte readByte(RandomAccessFile f) throws IOException {
        byte b = f.readByte();
        return b;
    }

    public static int readUByte(RandomAccessFile f) throws IOException {
        return readByte(f) & 0xff;
    }

    public static byte[] readBytes(RandomAccessFile f, int num) throws IOException {
        byte[] arr = new byte[num];
        f.read(arr);
        return arr;
    }

    public static char readLEChar(RandomAccessFile f) throws IOException {
        return (char) (readUByte(f) + (readUByte(f) << 8));
    }

    public static void writeLEChar(RandomAccessFile f, int v) throws IOException {
        f.writeByte((byte) (0xff & v));
        f.writeByte((byte) (0xff & (v >> 8)));
    }

    public static void writeLEChars(RandomAccessFile f, String s) throws IOException {
        for(int i = 0; i < s.length(); i++) {
            writeLEChar(f, s.charAt(i));
        }
    }

    /**
     * aka WORD
     *
     * @param f
     *
     * @return
     *
     * @throws IOException
     */
    public static short readLEShort(RandomAccessFile f) throws IOException {
        return (short) (readByte(f) + (readByte(f) << 8));
    }

    public static void writeLEShort(RandomAccessFile f, short value) throws IOException {
        f.writeByte(value);
        f.writeByte(value >> 8);
    }

    public static int readULEShort(RandomAccessFile f) throws IOException {
        return readUByte(f) + (readUByte(f) << 8);
    }

    public static void writeULEShort(RandomAccessFile f, short value) throws IOException {
        f.writeByte(value & 0xFF);
        f.writeByte((value >> 8) & 0xFF);
    }

    /**
     * aka DWORD
     *
     * @param f
     *
     * @return
     *
     * @throws IOException
     */
    public static int readLEInt(RandomAccessFile f) throws IOException {
        return readByte(f) + (readByte(f) << 8) + (readByte(f) << 16) + (readByte(f) << 24);
    }

    public static int readInt(RandomAccessFile f) throws IOException {
        return (readUByte(f) << 24) + (readUByte(f) << 16) + (readUByte(f) << 8) + readUByte(f);
    }

    public static int readULEInt(RandomAccessFile f) throws IOException {
        return readUByte(f) + (readUByte(f) << 8) + (readUByte(f) << 16) + (readUByte(f) << 24);
    }

    public static void writeLEInt(RandomAccessFile f, int value) throws IOException {
        //        ByteBuffer buffer = ByteBuffer.allocate(1000);
        //        buffer.order(ByteOrder.LITTLE_ENDIAN);
        //        buffer.putInt(i);
        //        f.write(buffer.array());
        f.writeByte(value & 0xFF);
        f.writeByte((value >> 8) & 0xFF);
        f.writeByte((value >> 16) & 0xFF);
        f.writeByte((value >> 24) & 0xFF);
    }

    /**
     * aka DWORD
     *
     * @param f
     *
     * @return
     *
     * @throws IOException
     */
    public static int readULong(RandomAccessFile f) throws IOException {
        return readUByte(f) + (readUByte(f) << 8) + (readUByte(f) << 16) + (readUByte(f) << 24);
    }

    public static void writeULong(RandomAccessFile f, int value) throws IOException {
        writeLEInt(f, value);
    }

    public static float readLEFloat(RandomAccessFile f) throws IOException {
        int intBits = readUByte(f) + (readUByte(f) << 8) + (readUByte(f) << 16) + (readUByte(f) << 24);
        return Float.intBitsToFloat(intBits);
    }
    //</editor-fold>

    public static String toBinaryString(short n) {
        return toBinaryString(n, 16);
    }

    public static String toBinaryString(int n) {
        return toBinaryString(n, 32);
    }

    public static String toBinaryString(long n) {
        return toBinaryString(n, 64);
    }

    public static String toBinaryString(long n, int radix) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < radix; i++) {
            sb.append("0");
        }
        for(int bit = 0; bit < radix; bit++) {
            if(((n >> bit) & 1) > 0) {
                sb.setCharAt(radix - 1 - bit, '1');
            }
        }
        return sb.toString();
    }

    public static int updateChecksumAddSpecial(int value) {
        int checksum = (value & 0xFF);
        checksum += (value >> 8 & 0xFF);
        checksum += (value >> 16 & 0xFF);
        checksum += (value >> 24 & 0xFF);
        return checksum;
    }

    public static int updateChecksumAdd(int value) {
        return value;
    }

    public static String hexDump(ByteBuffer buf) {
        int originalPosition = buf.position();
        byte[] byt = new byte[buf.limit()];
        buf.get(byt);
        buf.position(originalPosition);
        return Utils.hex(byt);
    }

    private DataUtils() {
    }

}