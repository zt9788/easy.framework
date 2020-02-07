package org.easy.framework.bloom;

import org.springframework.stereotype.Component;
import sun.jvm.hotspot.utilities.BitMap;

/**
 * @author: Zhangtong
 * @description:
 * @Date: 2020/2/7.
 */
@Component
public class BloomFilter {
    BitMap map;
    int bitSize = 1024*1024*50;
    byte[] bytes;
    public BloomFilter(){
        bytes = new byte[bitSize];
    }
    public BloomFilter(int bitSize){
        this.bitSize = bitSize;
        bytes = new byte[bitSize];
    }
    public int size(){
        return bitSize*8;
    }
    public synchronized void put(String value){
        int a = DefaultHashAlgorithms.APHash(value);
        int bb = DefaultHashAlgorithms.bernstein(value);
        int cc = DefaultHashAlgorithms.BKDRHash(value);
        int dd = DefaultHashAlgorithms.DEKHash(value);
        int ee = DefaultHashAlgorithms.DJBHash(value);
        int ff = DefaultHashAlgorithms.ELFHash(value);
        int gg = DefaultHashAlgorithms.java(value);
//        int hh = HashAlgorithms.intHash(value);
        int jj = DefaultHashAlgorithms.JSHash(value);
        int kk = DefaultHashAlgorithms.oneByOneHash(value);
        int ll = DefaultHashAlgorithms.PJWHash(value);
        int mm = DefaultHashAlgorithms.RSHash(value);
        int nn = DefaultHashAlgorithms.SDBMHash(value);
        System.out.println(a);
        System.out.println(bb);
        System.out.println(cc);
        System.out.println(dd);
        System.out.println(ff);
        System.out.println(gg);
        System.out.println(ee);
        System.out.println(jj);
        System.out.println(kk);
        System.out.println(ll);
        System.out.println(mm);
        System.out.println(nn);

        int len = a%8 != 0?a/8+1:a/8;
//        byte b = bytes[len];
        System.out.println(len);
//        System.out.println(c);
//        System.out.println(d);
        bytes[2] = (byte)a;

    }
    public void printBit(){
        for(int i=0;i<bitSize;i++){
            for(int j=0;j<8;j++) {
                System.out.print(((bytes[i]<<j) >>> (7-j))+",");
            }
            System.out.println();
        }
    }
    public static void main(String[] args) {
//        byte a = 78;
//        for (int i = 0; i < 8; i++) {
//            System.out.print((a >>> i) + ",");
//            System.out.print((a << i) + ",");
//        }
//        System.out.println();
//        //0100 0001 << 2 = 0001 0000 0100
//        //0100 0001 << 7 = 1000 0000
//        for (int i = 0; i < 8; i++) {
//            byte b = (byte) (a << i);
//            byte c = (byte) (b >>> 7);
//            System.out.print(Math.abs(c));
//        }
//        System.out.println("-----------");
//        for (int j = 0; j < 256; j++){
//            for (int i = 0; i < 8; i++) {
//                System.out.print((j >> (7 - i)) & 1);
//            }
//            System.out.println();
//        }

        BloomFilter f = new BloomFilter();
        f.put("string");
        f.printBit();
    }
    public synchronized boolean isExit(String value){
        int[] keys = {1,3,300};

        return false;
    }
}
