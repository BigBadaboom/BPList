package com.caverock;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

// Helpful sites on the bplist format:
// https://opensource.apple.com/source/CF/CF-1153.18/CFBinaryPList.c.auto.html
// https://medium.com/@karaiskc/understanding-apples-binary-property-list-format-281e6da00dbd
// https://doubleblak.com/blogPosts.php?id=3


public class BPList
{
    private static final int     HEADER_SIZE = 8;
    private static final int     FOOTER_SIZE = 16;

    // Apple date epoch.  Worked out using the following code:
    //Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    //cal.set(2001, Calendar.JANUARY, 1, 0, 0, 0);
    //cal.clear(Calendar.MILLISECOND);
    //cal.getTimeInMillis();
    private static final long  APPLE_DATE_EPOCH = 978307200000L;

    public static Result<Map<String, Object>>  decode(String filename)
    {
        File file = new File(filename);

        return decode(file);
    }


    public static Result<Map<String, Object>>  decode(File file)
    {
        HashMap<String, Object>  result;

        if (!file.canRead())
            return fileNotFound(file.getPath());
        else if (file.length() < (HEADER_SIZE + FOOTER_SIZE + 1))
            return invalidFile("File is not big enough to be a bplist file");
        else if (file.length() > Integer.MAX_VALUE)
            return invalidFile("File is too big to read into memory buffer");  // needs to fit in a byte array

        try {
            BytesReader  in = new BytesReader(Files.readAllBytes(file.toPath()));

            // Check the header
            // Check the special identifier string "bplist"
            in.seek(0);
            if (!in.readASCIIString(7).equals("bplist0"))
                return invalidFile("Invalid identifier");
            // Check version
            char version = in.readChar();
            // We don't car what the version is. It will normally be '0'.

            // Read the footer
            in.seek(in.length() - 32);
            in.skip(6);
            int offsetTableByteCount = in.readUnsignedByte();
            int objectRefByteCount = in.readUnsignedByte();

            long numObjects = in.readLongBE();
            long topObjectOffset = in.readLongBE();
            long offsetTableStart = in.readLongBE();

            /*
            System.out.println("version = '" + version + "'");
            System.out.println("offsetTableByteCount = " + offsetTableByteCount);
            System.out.println("objectRefByteCount = " + objectRefByteCount);
            System.out.println("numObjects = " + numObjects);
            System.out.println("topObjectOffset = " + topObjectOffset);
            System.out.format("offsetTableStart = 0x%x\n", offsetTableStart);
            */

            if (numObjects < 0)  // According to Apple, there should always be at least one object
                return invalidFile("File with no objects");
            if (topObjectOffset < 0 || topObjectOffset >= numObjects)
                return invalidFile("File with bad topObjectOffset");

            // Offset table pointer sanity checks
            long  offsetTableSize = numObjects * offsetTableByteCount;
            if (offsetTableStart < HEADER_SIZE ||
                    (offsetTableStart + offsetTableSize >= in.length() - FOOTER_SIZE))
                return invalidFile("Bad offset table");

            if (numObjects > Integer.MAX_VALUE)
                return error("Offset table too large to read: " + numObjects);

            if (offsetTableByteCount < 1 || offsetTableByteCount > 8)
                return error("Invalid offset table byte count: " + offsetTableByteCount);

            // Read the offset table
            long[]  offsetTable = new long[(int) numObjects];
            in.seek(offsetTableStart);
            readOffsetTableEntries(offsetTable, in, offsetTableByteCount);

            // Now read the actual objects
            if (objectRefByteCount < 1 || objectRefByteCount > 8)
                return error("Invalid object ref byte count: " + objectRefByteCount);

            Object obj = getObject(in, offsetTable, (int) topObjectOffset, objectRefByteCount);
            if (!(obj instanceof Map))
                return error("Root object was not a dictionary! We don't know how to decode these! Use this file to update code!");

            //result.put(ROOT_KEY_NAME, (Map<String, Object>) obj);
            result = (HashMap<String, Object>) obj;


            //System.out.println("result: " + new JSONObject(result));


            return new Result<>(result);
        }
        catch (Exception e)
        {
            return error("Error reading file: " + e.getMessage());
        }
    }


    //------------------------------------------------------------------------------------------------------------------
    // JSON output


    public static JsonObject  toJson(Map<String,Object> plist)
    {
        return jsonToMap(plist);
    }


    public static String  toJsonString(Map<String,Object> plist)
    {
        StringWriter stringWriter = new StringWriter();
        toJson(stringWriter, plist);
        return stringWriter.toString();
    }


    public static void  toJson(Writer writer, Map<String,Object> plist)
    {
        Map<String, Boolean> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory writerFactory = Json.createWriterFactory(config);
        JsonWriter jsonWriter = writerFactory.createWriter(writer);

        jsonWriter.write(toJson(plist));
        jsonWriter.close();
    }


    //==================================================================================================================



    private static Result<Map<String, Object>>  fileNotFound(String filename)
    {
        return new Result<>("Cannot read file \"" + filename + "\"");
    }


    private static Result<Map<String, Object>>  invalidFile(String msg)
    {
        return new Result<>((msg != null) ? "Invalid bplist file: " + msg : "Invalid bplist file");
    }


    private static Result<Map<String, Object>>  error(String msg)
    {
        return new Result<>(msg);
    }



    private static void readOffsetTableEntries(long[] offsetTable, BytesReader in, int offsetTableByteCount)
    {
        for (int i = 0; i < offsetTable.length; i++) {
            offsetTable[i] = readNByteNumber(in, offsetTableByteCount);
        }
    }


    // Read a offset or object table value of size 1..N bytes
    // We have already checked that N <= 8.
    private static long readNByteNumber(BytesReader in, int byteCount)
    {
        return switch (byteCount)
        {
            case 1 -> in.readUnsignedByte();
            case 2 -> in.readUnsignedShortBE();
            case 4 -> in.readUnsignedIntBE();
            case 8 -> in.readLongBE();
            default -> throw new RuntimeException(String.format("Request for integer of illegal size: %d bytes", byteCount));
        };
    }


    private static Object  getObject(BytesReader in,
                                     long[] offsetTable,
                                     int objectIndex,
                                     int objectRefByteCount)
    {
        in.seek(offsetTable[objectIndex]);
        int marker = in.readByte() & 0xff;
        if (marker == 0)
            return null;
        else if (marker == 8)
            return Boolean.FALSE;
        else if (marker == 9)
            return Boolean.TRUE;
        //else if (marker == 0x0f)  // "fill byte" = seems to not be ever used?

        int count = marker & 0xf;

        //System.out.println(String.format("Marker=%x count=%x",marker,count));
        switch ((marker >> 4) & 0xf)
        {
            case 1:  // integer
                if (count >= 4) {
                    // Some parsers I've seen suggest these 128bit integers are possible.
                    // They are not in the Apple "spec" (source) though
                    return new BigInteger( in.readByteArray(16) );
                }
                return readNByteNumber(in, 1 << count);

            case 2:  // real
                int fsz = 1 << count;
                if (fsz == 4)
                    return in.readFloatBE();
                else if (fsz == 8)
                    return in.readDoubleBE();
                else
                    throw new RuntimeException("Unsupported real number size");

            case 3:  // date
                if (count != 3)
                    throw new RuntimeException("Unexpected date object format");
                return Instant.ofEpochMilli(APPLE_DATE_EPOCH + (long) (1000f * in.readDoubleBE()));

            case 4:  // data
                if (count == 0xf)
                    count = extendedCount(in);
                return in.readUnsignedByteArray(count);

            case 5:  // string
                if (count == 0xf)
                    count = extendedCount(in);
                return in.readASCIIString(count);

            case 6:  // string
                if (count == 0xf)
                    count = extendedCount(in);
                return in.readUTF16StringBE(count);

            case 8:  // uid
                return readNByteNumber(in, count + 1);

            case 10:  // array
            case 12:  // set
                if (count == 0xf)
                    count = extendedCount(in);
                return parseArray(in, count, offsetTable, objectRefByteCount);

            case 13:  // dict
                if (count == 0xf)
                    count = extendedCount(in);
                return parseDict(in, count, offsetTable, objectRefByteCount);

            default:
                // The Apple source defines some other marker types.
                // But it seems like they might not have ever really been used, and are now no longer supported.
                throw new RuntimeException("Unexpected marker variant");
        }

    }


    private static int  extendedCount(BytesReader in)
    {
        int next = in.readUnsignedByte();
        int pow = next & 0xf;
        if ((next & 0xf0) != 0x10)
            throw new RuntimeException("Bad extended count marker: "+next);
        return (int) readNByteNumber(in, 1 << pow);
    }


    private static Object[]  parseArray(BytesReader in, int count, long[] offsetTable, int objectRefByteCount)
    {
        Object[]  array = new Object[count];
        for (int i = 0; i < count; i++) {
            int  vRef = (int) readNByteNumber(in, objectRefByteCount);
            in.pushPos();
            array[i] = getObject(in, offsetTable, vRef, objectRefByteCount);
            in.popPos();
        }
        return array;
    }


    private static HashMap<String, Object>  parseDict(BytesReader in, int count, long[] offsetTable, int objectRefByteCount)
    {
        HashMap<String, Object>  dict = new HashMap<>(count);
        // Read the key and value object numbers
        int  keyRefsStart = in.position();
        // How big the block of kRefs (and also the block of vRefs) is
        int  kvRefSz = count * objectRefByteCount;
        for (int i = 0; i < kvRefSz; i+=objectRefByteCount)
        {
            in.seek(keyRefsStart + i);
            // Get the key reference id
            int  kRef = (int) readNByteNumber(in, objectRefByteCount);
            // Get the actual key object
            Object k = getObject(in, offsetTable, kRef, objectRefByteCount);
            if (!(k instanceof String))
                throw new RuntimeException("Invalid dict key. Expected string.");
            // Jump now to the value reference id
            in.seek(keyRefsStart + kvRefSz + i);
            int  vRef = (int) readNByteNumber(in, objectRefByteCount);
            Object v = getObject(in, offsetTable, vRef, objectRefByteCount);
            dict.put((String) k, v);
        }
        return dict;
    }


    //==================================================================================================================



    private static JsonObject  jsonToMap(Map<String,Object> dict)
    {
        JsonObjectBuilder  builder = Json.createObjectBuilder();
        for (Map.Entry<String,Object> entry: dict.entrySet())
        {
            jsonObjectAddEntry(builder, entry.getKey(), entry.getValue());
        }
        return builder.build();
    }


    private static void jsonObjectAddEntry(JsonObjectBuilder builder, String key, Object val)
    {
        switch (val.getClass().getSimpleName())
        {
            case "Boolean" -> builder.add(key, (Boolean) val);
            case "Integer" -> builder.add(key, (int) val);
            case "Long" -> builder.add(key, (Long) val);
            case "Float" -> builder.add(key, (Float) val);
            case "Double" -> builder.add(key, (Double) val);
            case "String" -> builder.add(key, (String) val);
            case "Object[]" -> builder.add(key, jsonToArray((Object[]) val));
            case "HashMap" -> builder.add(key, jsonToMap((Map<String, Object>) val));
            case "int[]" -> builder.add(key, jsonToArray((int[]) val));
            case "BigInteger" -> builder.add(key, (BigInteger) val);
            // For JSON, we'll just return a date string in the ISO 8601 format
            case "Instant" -> builder.add(key, ZonedDateTime.ofInstant((Instant) val, ZoneId.of("GMT"))
                                                            .format(DateTimeFormatter.ISO_INSTANT));
            //default -> System.err.println("NYI: " + val.getClass().getSimpleName());
        }
    }


    private static JsonArray  jsonToArray(Object[] array)
    {
        JsonArrayBuilder  builder = Json.createArrayBuilder();
        for (Object entry: array) {
            jsonArrayAddEntry(builder, entry);
        }
        return builder.build();
    }


    private static JsonArray  jsonToArray(int[] array)
    {
        JsonArrayBuilder  builder = Json.createArrayBuilder();
        for (int entry: array) {
            jsonArrayAddEntry(builder, entry);
        }
        return builder.build();
    }


    private static void jsonArrayAddEntry(JsonArrayBuilder builder, Object val)
    {
        switch (val.getClass().getSimpleName())
        {
            case "Boolean" -> builder.add((Boolean) val);
            case "Integer" -> builder.add((int) val);
            case "Long" -> builder.add((Long) val);
            case "Float" -> builder.add((Float) val);
            case "Double" -> builder.add((Double) val);
            case "String" -> builder.add((String) val);
            case "Object[]" -> builder.add(jsonToArray((Object[]) val));
            case "HashMap" -> builder.add(jsonToMap((Map<String, Object>) val));
            case "int[]" -> builder.add(jsonToArray((int[]) val));
            case "BigInteger" -> builder.add((BigInteger) val);
            // For JSON, we'll just return a date string in the ISO 8601 format
            case "Instant" -> builder.add(ZonedDateTime.ofInstant((Instant) val, ZoneId.of("GMT"))
                                                       .format(DateTimeFormatter.ISO_INSTANT));
            //default -> System.err.println("NYI: " + val.getClass().getSimpleName());
        }
    }


}
