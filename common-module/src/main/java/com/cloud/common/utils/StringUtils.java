package com.cloud.common.utils;

import java.util.Collection;
import java.util.UUID;





public class StringUtils {

    





    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    





    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    





    public static boolean isBlank(String str) {
        if (str == null) {
            return true;
        }
        int len = str.length();
        if (len == 0) {
            return true;
        }
        for (int i = 0; i < len; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    





    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    





    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    





    public static String capitalize(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        char firstChar = str.charAt(0);
        if (Character.isTitleCase(firstChar)) {
            return str;
        }
        return Character.toTitleCase(firstChar) + str.substring(1);
    }

    





    public static String uncapitalize(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        char firstChar = str.charAt(0);
        if (Character.isLowerCase(firstChar)) {
            return str;
        }
        return Character.toLowerCase(firstChar) + str.substring(1);
    }

    




    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    






    public static String join(Collection<?> collection, String separator) {
        if (collection == null || collection.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object obj : collection) {
            if (first) {
                first = false;
            } else {
                sb.append(separator);
            }
            sb.append(obj);
        }
        return sb.toString();
    }

    






    public static boolean containsIgnoreCase(String str, String search) {
        if (str == null || search == null) {
            return false;
        }
        return str.toLowerCase().contains(search.toLowerCase());
    }

    

    





    public static String generateTraceId() {
        return uuid();
    }

    






    public static String generateLogId(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + uuid().substring(0, 8);
    }

    

    






    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    






    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        if (parts[0].length() <= 1) {
            return email;
        }
        return parts[0].charAt(0) + "***@" + parts[1];
    }

    






    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 8) {
            return cardNumber;
        }
        return cardNumber.substring(0, 4) + "****" + cardNumber.substring(cardNumber.length() - 4);
    }

    






    public static String sanitizeContent(String content) {
        if (content == null) {
            return null;
        }
        return content.replaceAll("\\d{11}", "***")  
                .replaceAll("\\d{15,19}", "***")  
                .replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "***@***.com");  
    }

    






    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    







    public static String generateUniqueUsername(String baseUsername, String prefix,
                                                java.util.function.Function<String, Boolean> checkFunction) {
        String candidateUsername = prefix + baseUsername;
        int suffix = 1;

        while (checkFunction.apply(candidateUsername)) {
            candidateUsername = prefix + baseUsername + "_" + suffix;
            suffix++;
            if (suffix > 1000) { 
                candidateUsername = prefix + baseUsername + "_" + System.currentTimeMillis();
                break;
            }
        }

        return candidateUsername;
    }
}
