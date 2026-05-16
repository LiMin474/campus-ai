package com.campus.user.util;

public final class CreditRules {

    private CreditRules() {
    }

    public static String levelLabel(int score) {
        if (score >= 120) {
            return "优秀";
        }
        if (score >= 80) {
            return "一般";
        }
        return "待提高";
    }
}
