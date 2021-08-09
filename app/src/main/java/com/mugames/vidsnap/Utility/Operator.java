package com.mugames.vidsnap.Utility;

import java.util.ArrayList;

public class Operator {
    public static ArrayList<String> operators = new ArrayList<String>(){
        {
            add("|");
            add("^");
            add("&");
            add(">>");
            add("<<");
            add("-");
            add("+");
            add("%");
            add("/");
            add("*");
        }
    };
    public static ArrayList<String> assign_operators = new ArrayList<String>(){
        {
            add("|=");
            add("^=");
            add("&=");
            add(">>=");
            add("<<=");
            add("-=");
            add("+=");
            add("%=");
            add("/=");
            add("*=");
            add("=");
        }
    };


    public static String Escape(String chr){
        return "\\"+chr;
    }


    public static int Operation(String operator, int a, int b) {
        int val = -1;
        if (operator.equals("=")) val = 10;
        for (String s : operators) {
            if (s.equals(operator)) {
                val = operators.indexOf(s);
                break;
            }
        }
        switch (val) {
            case 0:
                return a | b;
            case 1:
                return a ^ b;
            case 2:
                return a & b;
            case 3:
                return a >> b;
            case 4:
                return a << b;
            case 5:
                return a - b;
            case 6:
                return a + b;
            case 7:
                return a % b;
            case 8:
                return a / b;
            case 9:
                return a * b;
            case 10:
                return b;
        }

        return -10;

    }

    public static String Operation(String operator, String a, String b) {
        int val=-1;
        if(operator.equals("=")) val=10;
        for (String s : operators) {
            if (s.equals(operator)) {
                val = operators.indexOf(s);
                break;
            }
        }
        switch (val) {
            case 6:
                return a + b;
            case 10:
                return b;
        }
        return null;

    }
}