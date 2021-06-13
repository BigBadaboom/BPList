package app.veq;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Stack;

public class BytesReader
{
    byte[]          buf;
    int             pos = 0;
    Stack<Integer>  posStack = null;


    public BytesReader(byte[] buf)
    {
        if (buf == null)
            throw new NullPointerException("buf is null");
        this.buf = buf;
    }


    public int  length()
    {
        return buf.length;
    }


    public int  position()
    {
        return pos;
    }


    public int  remaining()
    {
        return buf.length - pos;
    }


    public boolean  seek(int nextPos)
    {
        if (nextPos < 0 || nextPos > buf.length)
            return false;
        this.pos = nextPos;
        return true;
    }


    public boolean  seek(long nextPos)
    {
        if (nextPos < 0 || nextPos > buf.length)
            return false;
        this.pos = (int) nextPos;
        return true;
    }


    public boolean  skip(int count)
    {
        return seek(pos + count);
    }


    public int  pushPos()
    {
        if (posStack == null)
            posStack = new Stack<>();
        posStack.push(pos);
        return pos;
    }


    public int  popPos()
    {
        if (posStack == null || posStack.empty())
            throw new ArrayIndexOutOfBoundsException("Read position stack is empty");
        pos = posStack.pop();
        return pos;
    }


    //------------------------------------------------------------------------------------------------------------------


    public int  read(byte[] out)
    {
        int len = Math.min(out.length, buf.length - pos);
        System.arraycopy(buf, pos, out, 0, len);
        return len;
    }


    public byte[]  readByteArray(int length)
    {
        if (remaining() < length)
            throw new ArrayIndexOutOfBoundsException("Not enough data remaining in buffer");
        if (pos == 0)
            return Arrays.copyOf(buf, length);

        byte[] result = new byte[length];
        if (length > 0) {
            System.arraycopy(buf, pos, result, 0, length);
            pos += length;
        }
        return result;
    }


    public int[]  readUnsignedByteArray(int length)
    {
        if (remaining() < length)
            throw new ArrayIndexOutOfBoundsException("Not enough data remaining in buffer");

        int[] result = new int[length];
        for (int i=0; i < length; i++) {
            result[i] = readUnsignedByte();
        }
        return result;
    }


    //------------------------------------------------------------------------------------------------------------------


    public byte readByte()
    {
        if (remaining() == 0)
            throw new ArrayIndexOutOfBoundsException("Not enough data remaining in buffer");
        return buf[pos++];
    }


    public int readUnsignedByte()
    {
        if (remaining() == 0)
            throw new ArrayIndexOutOfBoundsException("Not enough data remaining in buffer");
        return buf[pos++] & 0xff;   // ANDing with 0xff makes sure byte is treated as unsigned
    }


    public char readChar()
    {
        if (remaining() == 0)
            throw new ArrayIndexOutOfBoundsException("Not enough data remaining in buffer");
        return (char) (buf[pos++] & 0xff);
    }


    //------------------------------------------------------------------------------------------------------------------


    public short readShortLE()
    {
        if (remaining() < 2)
            throw new ArrayIndexOutOfBoundsException("Not enough bytes left to read a short");
        int result = buf[pos++] & 0xff;
        return (short) (result | (buf[pos++] << 8));
    }


    public int readUnsignedShortLE()
    {
        if (remaining() < 2)
            throw new ArrayIndexOutOfBoundsException("Not enough bytes left to read a short");
        int result = buf[pos++] & 0xff;
        return result | ((buf[pos++] & 0xff) << 8);  // ANDing with 0xff makes sure byte is treated as unsigned
    }


    public short readShortBE()
    {
        if (remaining() < 2)
            throw new ArrayIndexOutOfBoundsException("Not enough bytes left to read a short");
        int result = buf[pos++] << 8;
        return (short) (result | (buf[pos++] & 0xff));
    }


    public int readUnsignedShortBE()
    {
        if (remaining() < 2)
            throw new ArrayIndexOutOfBoundsException("Not enough bytes left to read a short");
        int result = (buf[pos++] & 0xff) << 8;
        return result | (buf[pos++] & 0xff);
    }


    //------------------------------------------------------------------------------------------------------------------


    public int readIntLE()
    {
        if (remaining() < 4)
            throw new ArrayIndexOutOfBoundsException("Not enough bytes left to read an int");
        int result = buf[pos++] & 0xff;
        result |= (buf[pos++] & 0xff) << 8;
        result |= (buf[pos++] & 0xff) << 16;
        return result | (buf[pos++] << 24);
    }


    public long readUnsignedIntLE()
    {
        if (remaining() < 4)
            throw new ArrayIndexOutOfBoundsException("Not enough bytes left to read an int");
        long result = buf[pos++] & 0xff;
        result |= (buf[pos++] & 0xff) << 8;
        result |= (buf[pos++] & 0xff) << 16;
        return result | ((buf[pos++] & 0xff) << 24);
    }


    public int readIntBE()
    {
        if (remaining() < 4)
            throw new ArrayIndexOutOfBoundsException("Not enough bytes left to read an int");
        int result = buf[pos++] << 24;
        result |= (buf[pos++] & 0xff) << 16;
        result |= (buf[pos++] & 0xff) << 8;
        return result | (buf[pos++] & 0xff);
    }


    public long readUnsignedIntBE()
    {
        if (remaining() < 4)
            throw new ArrayIndexOutOfBoundsException("Not enough bytes left to read an int");
        long result = (buf[pos++] & 0xff) << 24;
        result |= (buf[pos++] & 0xff) << 16;
        result |= (buf[pos++] & 0xff) << 8;
        return result | (buf[pos++] & 0xff);
    }


    //------------------------------------------------------------------------------------------------------------------


    public long readLongLE()
    {
        if (remaining() < 8)
            throw new ArrayIndexOutOfBoundsException("Not enough bytes left to read a long");
        long result = (long) (buf[pos++] & 0xff);
        result |= ((long) (buf[pos++] & 0xff)) << 8;
        result |= ((long) (buf[pos++] & 0xff)) << 16;
        result |= ((long) (buf[pos++] & 0xff)) << 24;
        result |= ((long) (buf[pos++] & 0xff)) << 32;
        result |= ((long) (buf[pos++] & 0xff)) << 40;
        result |= ((long) (buf[pos++] & 0xff)) << 48;
        return result | ((long) (buf[pos++] & 0xff)) << 56;
    }


    public long readLongBE()
    {
        if (remaining() < 8)
            throw new ArrayIndexOutOfBoundsException("Not enough bytes left to read a long");
        long result = ((long) (buf[pos++] &0xff)) << 56;
        result |= ((long) (buf[pos++] & 0xff)) << 48;
        result |= ((long) (buf[pos++] & 0xff)) << 40;
        result |= ((long) (buf[pos++] & 0xff)) << 32;
        result |= ((long) (buf[pos++] & 0xff)) << 24;
        result |= ((long) (buf[pos++] & 0xff)) << 16;
        result |= ((long) (buf[pos++] & 0xff)) << 8;
        return result | ((long) (buf[pos++] & 0xff));
    }


    //------------------------------------------------------------------------------------------------------------------


    public Float  readFloatLE()
    {
        return Float.intBitsToFloat(readIntLE());
    }


    public Float  readFloatBE()
    {
        return Float.intBitsToFloat(readIntBE());
    }


    public Double  readDoubleLE()
    {
        return Double.longBitsToDouble(readLongLE());
    }


    public Double  readDoubleBE()
    {
        return Double.longBitsToDouble(readLongBE());
    }


    //------------------------------------------------------------------------------------------------------------------


    public String  readASCIIString(int length)
    {
        if (remaining() < length)
            throw new ArrayIndexOutOfBoundsException("Not enough bytes left to read this string");
        int from = pos;
        pos += length;
        return new String(buf, from, length, StandardCharsets.US_ASCII);
    }


    public String  readUTF16StringLE(int length)
    {
        return readUTF16(length, StandardCharsets.UTF_16LE);
    }


    public String  readUTF16StringBE(int length)
    {
        return readUTF16(length, StandardCharsets.UTF_16BE);
    }


    private String  readUTF16(int length, Charset charSet)
    {
        int  sz = length * 2;  // two bytes per character
        if (remaining() < sz)
            throw new ArrayIndexOutOfBoundsException("Not enough bytes left to read this string");
        int from = pos;
        pos += sz;
        return new String(buf, from, sz, charSet);
    }


    //------------------------------------------------------------------------------------------------------------------



}
