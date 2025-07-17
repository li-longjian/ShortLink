package org.llj.shortlink.admin.utils;


import java.security.SecureRandom;
import java.util.Random;

public class RandomStringGenerator {
    // 定义可用字符集：包含数字和大小写字母
    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final Random RANDOM = new SecureRandom(); // 使用安全随机数生成器

    /**
     * 生成指定长度的随机字符串
     * @param length 需要生成的字符串长度
     * @return 随机字符串
     */
    public static String generate(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be a positive integer");
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            // 从字符集中随机选择一个字符
            int randomIndex = RANDOM.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(randomIndex));
        }
        return sb.toString();
    }

    /**
     * 生成6位随机字符串的便捷方法
     * @return 6位随机字符串
     */
    public static String generateSixDigits() {
        return generate(6);
    }


}
