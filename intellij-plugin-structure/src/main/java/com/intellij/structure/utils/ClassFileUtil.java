package com.intellij.structure.utils;

import com.intellij.structure.bytecode.ClassFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;

/**
 * @author Sergey Patrikeev
 */
public class ClassFileUtil {

  private static final int FIELD = 9;
  private static final int METH = 10;
  private static final int IMETH = 11;
  private static final int INT = 3;
  private static final int FLOAT = 4;
  private static final int LONG = 5;
  private static final int DOUBLE = 6;
  private static final int NAME_TYPE = 12;
  private static final int UTF8 = 1;
  private static final int HANDLE = 15;
  private static final int INDY = 18;

  private static short readShort(byte[] b, final int index) {
    return (short) (((b[index] & 0xFF) << 8) | (b[index + 1] & 0xFF));
  }

  private static int readUnsignedShort(byte[] b, final int index) {
    return ((b[index] & 0xFF) << 8) | (b[index + 1] & 0xFF);
  }

  @Nullable
  public static String extractClassName(@NotNull ClassFile classFile) {
    //TODO: proceed null
    return extractFromBytes(classFile);
  }

  @Nullable
  public static String extractFromBytes(@NotNull ClassFile classFile) {
    byte[] b = classFile.getBytes();

    // checks the class version
    if (readShort(b, 6) > Opcodes.V1_8) {
      throw new IllegalArgumentException();
    }
    // parses the constant pool
    final int items[] = new int[readUnsignedShort(b, 8)];
    int n = items.length;
    String strings[] = new String[n];
    int max = 0;
    int index = 10;
    for (int i = 1; i < n; ++i) {
      items[i] = index + 1;
      int size;
      switch (b[index]) {
        case FIELD:
        case METH:
        case IMETH:
        case INT:
        case FLOAT:
        case NAME_TYPE:
        case INDY:
          size = 5;
          break;
        case LONG:
        case DOUBLE:
          size = 9;
          ++i;
          break;
        case UTF8:
          size = 3 + readUnsignedShort(b, index + 1);
          if (size > max) {
            max = size;
          }
          break;
        case HANDLE:
          size = 4;
          break;
        default:
          size = 3;
          break;
      }
      index += size;
    }

    char[] c = new char[max]; // buffer used to read strings
    return readClass(b, index + 2, c, strings, items);
  }

  @Nullable
  private static String readClass(byte[] b, final int index, final char[] buf, String[] strings, int[] items) {
    // computes the start index of the CONSTANT_Class item in b
    // and reads the CONSTANT_Utf8 item designated by
    // the first two bytes of this CONSTANT_Class item
    return readUTF8(b, items[readUnsignedShort(b, index)], buf, strings, items);
  }

  @Nullable
  private static String readUTF8(byte[] b, int index, final char[] buf, String[] strings, int[] items) {
    int item = readUnsignedShort(b, index);
    if (index == 0 || item == 0) {
      return null;
    }
    String s = strings[item];
    if (s != null) {
      return s;
    }
    index = items[item];
    return strings[item] = readUTF(b, index + 2, readUnsignedShort(b, index), buf);
  }

  private static String readUTF(byte[] b, int index, final int utfLen, final char[] buf) {
    int endIndex = index + utfLen;
    int strLen = 0;
    int c;
    int st = 0;
    char cc = 0;
    while (index < endIndex) {
      c = b[index++];
      switch (st) {
        case 0:
          c = c & 0xFF;
          if (c < 0x80) { // 0xxxxxxx
            buf[strLen++] = (char) c;
          } else if (c < 0xE0 && c > 0xBF) { // 110x xxxx 10xx xxxx
            cc = (char) (c & 0x1F);
            st = 1;
          } else { // 1110 xxxx 10xx xxxx 10xx xxxx
            cc = (char) (c & 0x0F);
            st = 2;
          }
          break;

        case 1: // byte 2 of 2-byte char or byte 3 of 3-byte char
          buf[strLen++] = (char) ((cc << 6) | (c & 0x3F));
          st = 0;
          break;

        case 2: // byte 2 of 3-byte char
          cc = (char) ((cc << 6) | (c & 0x3F));
          st = 1;
          break;
      }
    }
    return new String(buf, 0, strLen);
  }


}
