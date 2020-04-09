package ru.itis.maletskov.first;

import javax.xml.bind.DatatypeConverter;

import static ru.itis.maletskov.first.Data.BLOCK_SIZE;
import static ru.itis.maletskov.first.Data.PI;
import static ru.itis.maletskov.first.Data.LINEAR_VECTOR;
import static ru.itis.maletskov.first.Data.REVERSE_PI;

public class Kuznechik {

    // массив для хранения констант
    public static byte[][] constants = new byte[32][16];
    // массив для хранения ключей
    public static byte[][] keys = new byte[10][64];

    public static byte[] key1 = {
            0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11, 0x00, (byte) 0xff, (byte) 0xee,
            (byte) 0xdd, (byte) 0xcc, (byte) 0xbb, (byte) 0xaa, (byte) 0x99, (byte) 0x88
    };

    public static byte[] key2 = {
            (byte) 0xef, (byte) 0xcd, (byte) 0xab, (byte) 0x89, 0x67, 0x45, 0x23, 0x01,
            0x10, 0x32, 0x54, 0x76, (byte) 0x98, (byte) 0xba, (byte) 0xdc, (byte) 0xfe
    };

    // функция X
    private static byte[] kuznechikX(byte[] a, byte[] b) {
        byte[] c = new byte[BLOCK_SIZE];
        for (int i = 0; i < BLOCK_SIZE; i++) {
            c[i] = (byte) (a[i] ^ b[i]);
        }
        return c;
    }

    // Функция S
    private static byte[] kuznechikS(byte[] inData) {
        byte[] outData = new byte[inData.length];
        for (int i = 0; i < BLOCK_SIZE; i++) {
            int data = inData[i];
            if (data < 0) {
                data = data + 256;
            }
            outData[i] = PI[data];
        }
        return outData;
    }

    // умножение в поле Галуа
    private static byte multipleGalua(byte a, byte b) {
        byte c = 0;
        byte hiBit;
        for (int i = 0; i < 8; i++) {
            if ((b & 1) == 1) {
                c ^= a;
            }
            hiBit = (byte) (a & 0x80);
            a <<= 1;
            if (hiBit < 0) {
                a ^= 0xc3; //полином  x^8+x^7+x^6+x+1
            }
            b >>= 1;
        }
        return c;
    }

    // функция R сдвигает данные и реализует уравнение, представленное для расчета L-функции
    private static byte[] kuznechikR(byte[] state) {
        byte a15 = 0;
        byte[] internal = new byte[16];
        for (int i = 15; i >= 0; i--) {
            if (i == 0) {
                internal[15] = state[i];
            } else {
                internal[i - 1] = state[i];
            }
            a15 ^= multipleGalua(state[i], LINEAR_VECTOR[i]);
        }
        internal[15] = a15;
        return internal;
    }

    private static byte[] kuznechikL(byte[] inData) {
        byte[] internal = inData;
        for (int i = 0; i < 16; i++) {
            internal = kuznechikR(internal);
        }
        return internal;
    }

    // функция S^(-1)
    private static byte[] reverseS(byte[] inData) {
        byte[] outData = new byte[inData.length];
        for (int i = 0; i < BLOCK_SIZE; i++) {
            int data = inData[i];
            if (data < 0) {
                data = data + 256;
            }
            outData[i] = REVERSE_PI[data];
        }
        return outData;
    }

    private static byte[] reverseR(byte[] state) {
        byte a0 = state[15];
        byte[] internal = new byte[16];
        for (int i = 1; i < 16; i++) {
            internal[i] = state[i - 1];
            a0 ^= multipleGalua(internal[i], LINEAR_VECTOR[i]);
        }
        internal[0] = a0;
        return internal;
    }

    private static byte[] reverseL(byte[] inData) {
        byte[] internal = inData;
        for (int i = 0; i < 16; i++) {
            internal = reverseR(internal);
        }
        return internal;
    }

    // функция расчета констант
    private static void calculateConstants() {
        byte[][] iterNum = new byte[32][16];
        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < BLOCK_SIZE; j++) {
                iterNum[i][j] = 0;
            }
            iterNum[i][0] = (byte) (i + 1);
        }
        for (int i = 0; i < 32; i++) {
            constants[i] = kuznechikL(iterNum[i]);
        }
    }

    // функция, выполняющая преобразования ячейки Фейстеля
    private static byte[][] kuznechikFeystel(byte[] inKey1, byte[] inKey2, byte[] iterConst) {
        byte[] internal;
        internal = kuznechikX(inKey1, iterConst);
        internal = kuznechikS(internal);
        internal = kuznechikL(internal);
        byte[] outKey1 = kuznechikX(internal, inKey2);
        return new byte[][]{outKey1, inKey1};
    }

    // функция расчета раундовых ключей
    public static void kuznechikExpandKey(byte[] key1, byte[] key2) {
        byte[][] iter12 = new byte[2][];
        byte[][] iter34;
        calculateConstants();
        keys[0] = key1;
        keys[1] = key2;
        iter12[0] = key1;
        iter12[1] = key2;
        for (int i = 0; i < 4; i++) {
            iter34 = kuznechikFeystel(iter12[0], iter12[1], constants[8 * i]);
            iter12 = kuznechikFeystel(iter34[0], iter34[1], constants[1 + 8 * i]);
            iter34 = kuznechikFeystel(iter12[0], iter12[1], constants[2 + 8 * i]);
            iter12 = kuznechikFeystel(iter34[0], iter34[1], constants[3 + 8 * i]);
            iter34 = kuznechikFeystel(iter12[0], iter12[1], constants[4 + 8 * i]);
            iter12 = kuznechikFeystel(iter34[0], iter34[1], constants[5 + 8 * i]);
            iter34 = kuznechikFeystel(iter12[0], iter12[1], constants[6 + 8 * i]);
            iter12 = kuznechikFeystel(iter34[0], iter34[1], constants[7 + 8 * i]);
            keys[2 * i + 2] = iter12[0];
            keys[2 * i + 3] = iter12[1];
        }
    }

    // функция шифрования блока
    public byte[] kuznechikEncrypt(byte[] blk) {
        byte[] outBlk = blk;
        for (int i = 0; i < 9; i++) {
            outBlk = kuznechikX(keys[i], outBlk);
            outBlk = kuznechikS(outBlk);
            outBlk = kuznechikL(outBlk);
        }
        outBlk = kuznechikX(outBlk, keys[9]);
        return outBlk;
    }

    //функция расшифрования блока
    public byte[] kuznechikDecrypt(byte[] blk) {
        byte[] outBlk = kuznechikX(blk, keys[9]);
        for (int i = 8; i >= 0; i--) {
            outBlk = reverseL(outBlk);
            outBlk = reverseS(outBlk);
            outBlk = kuznechikX(keys[i], outBlk);
        }
        return outBlk;
    }

    public Kuznechik() {
        kuznechikExpandKey(key1, key2);
    }

    public static void main(String[] args) {
        String openText = "00000000000000000000000000000000";
        System.out.println("Text block: " + openText);
        Kuznechik kuznechik = new Kuznechik();
        byte[] encryptBlock = kuznechik.kuznechikEncrypt(DatatypeConverter.parseHexBinary(openText));
        System.out.println("Encrypt block: " + DatatypeConverter.printHexBinary(encryptBlock));
        byte[] decryptBlock = kuznechik.kuznechikDecrypt(encryptBlock);
        System.out.println("Decrypt block: " + DatatypeConverter.printHexBinary(decryptBlock));
    }
}
