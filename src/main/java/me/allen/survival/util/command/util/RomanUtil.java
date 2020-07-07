package me.allen.survival.util.command.util;

import java.util.TreeMap;

/**
 * Convert Integer to Roman Numerals
 * Credit to https://stackoverflow.com/questions/12967896/converting-integers-to-roman-numerals-java
 */
public class RomanUtil {
    private final static TreeMap<Integer, String> intToRomanMap = new TreeMap<>();

    static {
        intToRomanMap.put(1000, "M");
        intToRomanMap.put(900, "CM");
        intToRomanMap.put(500, "D");
        intToRomanMap.put(400, "CD");
        intToRomanMap.put(100, "C");
        intToRomanMap.put(90, "XC");
        intToRomanMap.put(50, "L");
        intToRomanMap.put(40, "XL");
        intToRomanMap.put(10, "X");
        intToRomanMap.put(9, "IX");
        intToRomanMap.put(5, "V");
        intToRomanMap.put(4, "IV");
        intToRomanMap.put(1, "I");
    }

    public static String toRoman(int number) {
        int l = intToRomanMap.floorKey(number);
        if (number == l) return intToRomanMap.get(number);

        return intToRomanMap.get(l) + toRoman(number - l);
    }

}
