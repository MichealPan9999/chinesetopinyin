package com.example.editablealphalist.utils;

import android.content.Context;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pinyin4JUtil {

    private Context mContext;
    Map<String, List<String>> pinyinMap = new HashMap<String, List<String>>();
    public Pinyin4JUtil(Context context) {
        mContext = context;
        initDictionary(context);
    }

    public String getPingYin(String src) {
        char[] inputChar = null;
        inputChar = src.toCharArray();
        int inputCharLength = inputChar.length;
        String[] piword = new String[inputCharLength];
        HanyuPinyinOutputFormat hanyuPinyinOutputFormat = new HanyuPinyinOutputFormat();
        hanyuPinyinOutputFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        //带拼音状态(toneType和charType必须成对使用，否则会异常)
        //hanyuPinyinOutputFormat.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);
        // hanyuPinyinOutputFormat.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
        hanyuPinyinOutputFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        hanyuPinyinOutputFormat.setVCharType(HanyuPinyinVCharType.WITH_V);
        String piStr = "";
        try {
            for (int i = 0; i < inputCharLength; i++) {
                // 判断是否为汉字字符
                if (java.lang.Character.toString(inputChar[i]).matches("[\\u4E00-\\u9FA5]+")) {
                    piword = PinyinHelper.toHanyuPinyinStringArray(inputChar[i], hanyuPinyinOutputFormat);
                    piStr += piword[0];
                } else
                    piStr += java.lang.Character.toString(inputChar[i]);
            }
            // System.out.println(t4);
            return piStr;
        } catch (BadHanyuPinyinOutputFormatCombination e1) {
            e1.printStackTrace();
        }
        return piStr;
    }

    // 返回中文的首字母
    public String getPinYinHeadChar(String str) {

        String convert = "";
        for (int j = 0; j < str.length(); j++) {
            char word = str.charAt(j);
            String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(word);
            if (pinyinArray != null) {
                convert += pinyinArray[0].charAt(0);
            } else {
                convert += word;
            }
        }
        return convert;
    }

    // 返回中文的首字母
    public String getPinYinFirstWordHeadChar(String str) {

        String convert = "";
        char word = str.charAt(0);
        String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(word);
        if (pinyinArray != null) {
            convert += pinyinArray[0].charAt(0);
        } else {
            convert += word;
        }
        return convert;
    }

    // 将字符串转移为ASCII码
    public String getCnASCII(String cnStr) {
        StringBuffer strBuf = new StringBuffer();
        byte[] bGBK = cnStr.getBytes();
        for (int i = 0; i < bGBK.length; i++) {
            strBuf.append(Integer.toHexString(bGBK[i] & 0xff));
        }
        return strBuf.toString();
    }


    private HashMap<String, String> initDictionary(Context context) {
        String fileName = "py4j.txt";
        InputStreamReader inputReader = null;
        BufferedReader bufferedReader = null;
        HashMap<String, String> polyphoneMap = new HashMap<String, String>();
        try {
            inputReader = new InputStreamReader(context.getResources().getAssets().open(fileName), "UTF-8");
            bufferedReader = new BufferedReader(inputReader);
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                //Log.d(TAG,"======"+line);
                if (line != null) {
                    String[] arr = line.split("#");
                    String pinyin = arr[0];
                    String chinese = arr[1];
                    if (chinese != null) {
                        String[] strs = chinese.split(" ");
                        List<String> list = Arrays.asList(strs);
                        pinyinMap.put(pinyin, list);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 汉字转拼音 最大匹配优先 区分多音字最优搭配
     *
     * @param chinese
     * @return
     */
    public String convertChineseToPinyin(String chinese) {

        StringBuffer pinyin = new StringBuffer();
        HanyuPinyinOutputFormat defaultFormat = getHanyuPinyinOutputFormat(false);
        char[] arr = chinese.toCharArray();
        int resultPos = 0;
        for (int i = 0; i < arr.length; i++) {
            char ch = arr[i];
            if (ch > 128) { // 非ASCII码
                // 取得当前汉字的所有全拼
                try {
                    String[] results = PinyinHelper.toHanyuPinyinStringArray(ch, defaultFormat);
                    if (results == null) {  //非中文
                        //return "";
                        pinyin.append(ch);
                    } else {
                        int len = results.length;
                        if (len == 1) { // 不是多音字
                            String[] pinyinArray = getHanyuStrings(true, ch);
                            pinyin.append(pinyinArray[0]);
                        } else if (results[0].equals(results[1])) {    //非多音字 有多个音，取第一个
                            //pinyin.append(results[0]);
                            String[] pinyinArray = getHanyuStrings(true, ch);
                            pinyin.append(pinyinArray[0]);
                        } else { // 多音字
                            int length = chinese.length();
                            boolean flag = false;
                            String s = null;
                            List<String> keyList = null;
                            for (int x = 0; x < len; x++) {
                                String py = results[x];
                                keyList = pinyinMap.get(py.toLowerCase());
                                resultPos = x;
                                if (i + 3 <= length) {   //后向匹配2个汉字  大西洋
                                    s = chinese.substring(i, i + 3);
                                    if (keyList != null && (keyList.contains(s))) {
                                        flag = true;
                                        break;
                                    }
                                }
                                if (i + 2 <= length) {   //后向匹配 1个汉字  大西
                                    s = chinese.substring(i, i + 2);
                                    if (keyList != null && (keyList.contains(s))) {
                                        flag = true;
                                        break;
                                    }
                                }
                                if ((i - 2 >= 0) && (i + 1 <= length)) {  // 前向匹配2个汉字 龙固大
                                    s = chinese.substring(i - 2, i + 1);
                                    if (keyList != null && (keyList.contains(s))) {
                                        flag = true;
                                        break;
                                    }
                                }
                                if ((i - 1 >= 0) && (i + 1 <= length)) {  // 前向匹配1个汉字   固大
                                    s = chinese.substring(i - 1, i + 1);
                                    if (keyList != null && (keyList.contains(s))) {
                                        flag = true;
                                        break;
                                    }
                                }
                                if ((i - 1 >= 0) && (i + 2 <= length)) {  //前向1个，后向1个      固大西
                                    s = chinese.substring(i - 1, i + 2);
                                    if (keyList != null && (keyList.contains(s))) {
                                        flag = true;
                                        break;
                                    }
                                }
                            }
                            if (!flag) {//都没有找到，匹配默认的 读音  大
                                s = String.valueOf(ch);
                                for (int x = 0; x < len; x++) {
                                    String py = results[x];
                                    keyList = pinyinMap.get(py.toLowerCase());
                                    if (keyList != null && (keyList.contains(s))) {
                                        String[] pinyinArray = getHanyuStrings(true, ch);
                                        pinyin.append(pinyinArray[x]);
                                        break;
                                    }
                                }
                            } else {
                                String[] pinyinArray = getHanyuStrings(true, ch);
                                pinyin.append(pinyinArray[resultPos]);
                            }
                        }
                    }

                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    e.printStackTrace();
                }
            } else {
                pinyin.append(arr[i]);
            }
        }
        return pinyin.toString();
    }

    private String[] getHanyuStrings(boolean withTone, char ch) {
        HanyuPinyinOutputFormat format = getHanyuPinyinOutputFormat(withTone);
        String[] pinyinArray = new String[0];
        try {
            pinyinArray = PinyinHelper.toHanyuPinyinStringArray(ch, format);
        } catch (BadHanyuPinyinOutputFormatCombination badHanyuPinyinOutputFormatCombination) {
            badHanyuPinyinOutputFormatCombination.printStackTrace();
        }
        return pinyinArray;
    }
    /**
     * 获取拼音初始汉语拼音格式化实例
     *
     * @param withTone
     * @return
     */
    private HanyuPinyinOutputFormat getHanyuPinyinOutputFormat(boolean withTone) {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        if (withTone) {
            //直接用音标符
            format.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);// WITHOUT_TONE：无音标 (xing)
            //format.setToneType(HanyuPinyinToneType.WITH_TONE_NUMBER);// WITH_TONE_NUMBER：1-4数字表示英标 (xing2)
            //直接用ü (nü)
            format.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);//WITH_V：用v表示ü (nv) //WITH_U_AND_COLON：用”u:”表示ü (nu:)
            format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        } else {
            //直接用音标符
            format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
            format.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
            format.setCaseType(HanyuPinyinCaseType.UPPERCASE);
        }
        return format;
    }

}
