/*
 *  This file is part of VidSnap.
 *
 *  VidSnap is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  VidSnap is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with VidSnap.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.mugames.vidsnap.utility;

import com.mugames.vidsnap.utility.UtilityInterface.JSInterface;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mugames.vidsnap.utility.UtilityClass.*;

import android.util.Log;

/**
 * Used to decrypt the signature by Youtube and only used by {@link com.mugames.vidsnap.extractor.YouTube}
 */
public class JSInterpreter {

    public static final String NAME_REGEX = "[a-zA-Z_$][a-zA-Z_$0-9]*";

    public String code;
    public Hashtable<String, Hashtable<String, JSInterface>> objects;
    public Hashtable<String, JSInterface> functions;
    LinkedHashMap<String, Object> localVars = new LinkedHashMap<>();


    public JSInterpreter(String code, Hashtable<String, Hashtable<String, JSInterface>> objects) {
        this.code = code;
        if (objects == null) this.objects = new Hashtable<>();
        else this.objects = objects;
        this.functions = new Hashtable<>();

    }

    public JSInterface extractFunction(String functionName) {
        return extractFunctionFromCode(extractFunctionCode(functionName));
    }

    public JSFunctionCode extractFunctionCode(String functionName) {
        functionName = Matcher.quoteReplacement(functionName);
        Matcher matcher = Pattern.compile(String.format(
                "(?:function\\s+%s|[{;,]\\s*%s\\s*=\\s*function|var\\s+%s\\s*=\\s*function)\\s*\\(([^)]*)\\)\\s*(\\{(?:(?!\\};)[^\\\"]|\\\"([^\\\"]|\\\\\\\")*\\\")+\\})",
                functionName,
                functionName,
                functionName
        )).matcher(code);
        if (matcher.find()){
            String localCode = separateAtParen(matcher.group(2),"}").get(0);
            return new JSFunctionCode(matcher.group(1).split(","), localCode);
        }
        throw new IllegalArgumentException("Function " + functionName + " not found");
    }


    public JSInterface extractFunctionFromCode(JSFunctionCode functionCode) {
        String jsCode = functionCode.code;
        while (true) {
            Matcher mobj = Pattern.compile("function\\(([^)]*)\\)\\s*\\{")
                    .matcher(jsCode);
            if (!mobj.find())
                break;
            int start = mobj.start();
            int bodyStart = mobj.end();
            ArrayList<String> separated = separateAtParen(jsCode.substring(bodyStart - 1), "}");
            String body = separated.get(0);
            String remaining = separated.get(1);
            Log.d("JSIP", "extractFunctionFromCode: " + code.substring(0, start));
            JSInterface function = extractFunctionFromCode(new JSFunctionCode(mobj.group(1).split(","), body));
            String name = namedObject(function);
            functions.put(name, function);
            jsCode = jsCode.substring(0, start) + name + remaining;
        }
        return buildFunction(functionCode.args, jsCode);
    }

    int nameObjectCounter = 0;

    String namedObject(Object obj) {
        nameObjectCounter++;
        String name = String.format("vidsnap_interp_obj%s", nameObjectCounter);
        localVars.put(name, obj);
        return name;
    }

    ArrayList<String> separateAtParen(String exp, String delimiter) {
        ArrayList<String> separated = separate(exp, delimiter, 1);
        if (separated.size() < 2)
            throw new IllegalArgumentException(String.format("No terminating paren %s in %s", delimiter, exp));
        return new ArrayList<>(Arrays.asList(
                separated.get(0).substring(1), separated.get(1).trim()
        ));
    }

    ArrayList<String> separate(String exp, String delimiter, int maxSplit) {
        ArrayList<String> matchingParen = new ArrayList<>(Arrays.asList(
                "(", "{", "["
        ));
        LinkedHashMap<String, Integer> counter = new LinkedHashMap<>();
        counter.put(")", 0);
        counter.put("}", 0);
        counter.put("]", 0);

        int start = 0;
        int splits = 0;
        int pos = 0;
        int delimiterLength = delimiter.length() - 1;

        ArrayList<String> stringArrayList = new ArrayList<>();

        int idx = -1;
        for (char chr : exp.toCharArray()) {
            idx++;
            String ch = String.valueOf(chr);
            if (matchingParen.contains(ch)) {
                int index = matchingParen.indexOf(ch);
                String key = String.valueOf(counter.keySet().toArray()[index]);
                counter.put(
                        key,
                        counter.get(key) + 1
                );
            } else if (counter.containsKey(ch)) {
                counter.put(
                        ch,
                        counter.get(ch) - 1
                );
            }
            if (!ch.equals(String.valueOf(delimiter.charAt(pos))) || any(counter.values())) {
                pos = 0;
                continue;
            } else if (pos != delimiterLength) {
                pos += 1;
                continue;
            }
            stringArrayList.add(exp.substring(start, idx - delimiterLength));
            start = idx + 1;
            pos = 0;
            splits++;
            if (maxSplit >= 0 && splits >= maxSplit) break;
        }
        stringArrayList.add(exp.substring(start));
        return stringArrayList;
    }

    <T> boolean any(Collection<T> collection) {
        if (collection.isEmpty()) return false;
        int zeroCount = 0;
        for (T item : collection) {
            if (item instanceof Boolean) {
                if (!(Boolean) item) return false;
            } else if (item instanceof Number) {
                if (item.equals(0)) {
                    zeroCount++;
                }
            }
        }
        return zeroCount != collection.size();
    }

    JSInterface buildFunction(String[] argnames, String function_code) {
        return values -> {
            for (int i = 0; i < argnames.length; i++) {
                localVars.put(argnames[i], values[i]);
            }
            JSResultArray resultArray = new JSResultArray();
            for (String stmt : function_code.replace("\n", "").split(";")) {
                resultArray = interpretStatement(stmt, 100);
                if (resultArray.abort) break;
            }
            return resultArray.res;
        };
    }


    public JSResultArray interpretStatement(String stmt, int recurLimit) {
        String expr;
        JSResultArray resultArray = new JSResultArray();
        if (recurLimit < 0) {
            try {
                throw new Exception("Recursion limit reached");
            } catch (Exception e) {
            }
        }
        ArrayList<String> subStatements = separate(stmt, ";", -1);
        stmt = subStatements.get(subStatements.size() - 1);
        subStatements.remove(subStatements.size() - 1);
        for (String subStmt : subStatements) {
            JSResultArray array = interpretStatement(subStmt, recurLimit - 1);
            if (array.abort) return array;
        }

        resultArray.abort = false;
        stmt = stmt.replaceAll("^\\s+", "");
        Pattern pattern = Pattern.compile("var\\s");
        Matcher stmt_m = pattern.matcher(stmt);
        if (stmt_m.find()) expr = stmt.substring(stmt_m.group(0).length());
        else {
            Pattern return_p = Pattern.compile("return(?:\\s+|$)");
            Matcher return_m = return_p.matcher(stmt);
            if (return_m.find()) {
                expr = stmt.substring(return_m.group(0).length());
                resultArray.abort = true;
            } else {
                expr = stmt;
            }
        }
        resultArray.res = interpretExpression(expr, recurLimit);
        return resultArray;
    }

    Object interpretExpression(String expr, int recurLimit) {
        Object obj = new Hashtable<>();
        Hashtable<String, JSInterface> obj_m = new Hashtable<>();
        List<Object> argVal = new ArrayList<>();
        expr = expr.trim();
        if (expr.isEmpty())
            return null;
        if (expr.startsWith("{")) {
            ArrayList<String> innerOuter = separateAtParen(expr, "}");
            JSResultArray resultArray = interpretStatement(innerOuter.get(0), recurLimit - 1);
            String outer = null;
            try {
                outer = innerOuter.get(1);
            } catch (ArrayIndexOutOfBoundsException e) {
            }
            if (outer != null || resultArray.abort) return innerOuter.get(0);
            else {

            }

        }
        if (expr.startsWith("(")) {
            int parens_Count = 0;
            Pattern pattern = Pattern.compile("[()]");
            Matcher matcher = pattern.matcher(expr);
            while (matcher.find()) {
                if (matcher.group(0).equals("(")) parens_Count++;
                else {
                    parens_Count -= 1;
                    if (parens_Count == 0) {
                        String sub_expr = expr.substring(1, matcher.start());
                        String sub_res = (String) interpretExpression(sub_expr, recurLimit);
                        String remaining_expr = expr.substring(matcher.end()).trim();
                        if (remaining_expr.isEmpty()) return sub_res;
                        else expr = sub_res + remaining_expr;
                        break;
                    }
                    //else {/*wrong JS code*/}

                }
            }
        }
        if (expr.startsWith("[")) {
            ArrayList<String> innerOuter = separateAtParen(expr, "]");
            for (String item : separate(innerOuter.get(0), ",", -1)) {
                String name = namedObject(interpretExpression(item, recurLimit));
                expr = name + innerOuter.get(1);
            }
        }
        Matcher matcher = Pattern.compile("try\\s*").matcher(expr);
        if (matcher.find()) {
            ArrayList<String> arrayList;
            if (expr.charAt(matcher.end()) == '{') {
                arrayList = separateAtParen(expr.substring(matcher.end()), "}");
            } else {
                arrayList = new ArrayList<>(Arrays.asList(expr.substring(matcher.end()), ""));
            }
            String tryExpr = arrayList.get(0);
            expr = arrayList.get(1);
            JSResultArray resultArray = interpretStatement(tryExpr, recurLimit - 1);
            if (resultArray.abort) return resultArray;
            return interpretStatement(expr, recurLimit - 1);
        }
        matcher = Pattern.compile("(?:(catch)|(for)|(switch))\\s*\\(").matcher(expr);
        if (matcher.find()) {
            if (matcher.group(1) != null) {
                //Skip catch block
                ArrayList<String> list = separateAtParen(expr, "}");
                return interpretStatement(list.get(1), recurLimit - 1);
            } else if (matcher.group(2) != null) {
                //For loop
                ArrayList<String> stringArrayList = separateAtParen(expr.substring(matcher.end() - 1), ")");
                String constructor = stringArrayList.get(0);
                String remaining = stringArrayList.get(1);
                String body;
                if (remaining.startsWith("{")) {
                    ArrayList<String> list = separateAtParen(remaining, "}");
                    expr = list.get(1);
                    body = list.get(0);
                } else {
                    matcher = Pattern.compile("switch\\s*\\(").matcher(remaining);
                    if (matcher.find()) {
                        ArrayList<String> list = separateAtParen(remaining.substring(matcher.end() - 1), ")");
                        String switchVal = list.get(0);
                        remaining = list.get(1);
                        list = separateAtParen(remaining, "}");
                        body = list.get(0);
                        expr = list.get(1);
                        body = String.format("switch(%s){%s}", switchVal, body);
                    } else {
                        body = remaining;
                        expr = "";
                    }
                    ArrayList<String> strings = separate(constructor, ";", -1);
                    String start = strings.get(0);
                    String cndn = strings.get(1);
                    String increment = strings.get(2);
                    if (interpretStatement(start, recurLimit - 1).abort)
                        throw new IllegalStateException("Premature return in the initialization of a for loop in " + constructor);
                    while (true) {
                        if (!interpretStatement(cndn, recurLimit - 1).abort) break;
                        try {
                            JSResultArray resultArray = interpretStatement(body, recurLimit - 1);
                            if (resultArray.abort) return resultArray;
                        } catch (JSBreak jsBreak) {
                            break;
                        } catch (JSContinue jsContinue) {
                        }
                        if (interpretStatement(increment, recurLimit - 1).abort)
                            throw new IllegalStateException("Premature return in the initialization of a for loop in " + constructor);
                        return interpretStatement(expr, recurLimit - 1);
                    }
                }
            } else if (matcher.group(3) != null) {
                //switch block
                ArrayList<String> stringArrayList = separateAtParen(expr.substring(matcher.end() - 1), ")");
                String switchVal = stringArrayList.get(0);
                String remaining = stringArrayList.get(1);
                switchVal = String.valueOf(interpretExpression(switchVal, recurLimit));
                stringArrayList = separateAtParen(remaining, "}");
                String body = stringArrayList.get(0);
                expr = stringArrayList.get(1);
                String[] items = body.replaceAll("default:", "case default:").split("case ");
                for (boolean def : new boolean[]{false, true}) {
                    boolean matched = false;
                    for (int i = 1; i < items.length; i++) {
                        String cas = null;
                        String stmt = null;
                        ArrayList<String> tempsList = separate(items[i], ":", 1);
                        for (int j = 0; j < tempsList.size(); j++) {
                            if (j == 0) {
                                cas = tempsList.get(j).trim();
                            } else {
                                stmt = tempsList.get(j).trim();
                            }
                        }
                        if (def) {
                            matched = matched || cas.equals("default");
                        } else if (!matched) {
                            matched = !cas.equals("default") &&
                                    switchVal.equals(interpretExpression(cas, recurLimit));
                        }
                        if (!matched)
                            continue;
                        try {
                            JSResultArray jsResultArray = interpretStatement(stmt, recurLimit - 1);
                            if (jsResultArray.abort) return jsResultArray;
                        } catch (JSBreak e) {
                            break;
                        }
                    }
                    if (matched) break;
                }
                return interpretStatement(expr, recurLimit - 1);
            }
        }
        //Comma separated statements
        ArrayList<String> subExpression = separate(expr, ",", -1);
        try {
            expr = subExpression.get(subExpression.size() - 1).trim();
            subExpression.remove(subExpression.size() - 1);
        } catch (ArrayIndexOutOfBoundsException e) {
            expr = "";
        }
        for (String subExpr : subExpression) {
            interpretExpression(subExpr, recurLimit);
        }

        matcher = Pattern.compile("(\\\\+\\\\+|--)([a-zA-Z_$][a-zA-Z_$0-9]*)|([a-zA-Z_$][a-zA-Z_$0-9]*)(\\\\+\\\\+|--)").matcher(expr);
        while (matcher.find()) {
            String var = matcher.group(2);
            if (var == null) var = matcher.group(3);
            int start = matcher.start();
            int end = matcher.end();
            String sign = matcher.group(1);
            if (sign == null) sign = matcher.group(4);
            String ret = String.valueOf(localVars.get(var));
            expr = expr.substring(0, start) + ret + expr.substring(end);
        }

        for (String s : Operator.assign_operators) {
            Pattern p = Pattern.compile(String.format("(%s)(?:\\[([^\\]]+?)\\])?\\s*\\%s(.*)$", NAME_REGEX, s));
            Matcher m = p.matcher(expr);
            if (!m.find()) continue;
            char[] right_val = null;
            Object o = interpretExpression(m.group(3), recurLimit - 1);
            try {
                right_val = stringToCharArray((String) o);
            } catch (Exception e) {
                try {
                    right_val = stringToCharArray(charArrayToSting((char[]) o));
                } catch (Exception exception) {
                    if (o instanceof JSInterface){
                        Object res = ((JSInterface) o);
                    }else {
                        right_val = new char[1];
                        right_val[0] = (char) o;
                    }
                }
            }

            if (m.group(2) != null) {
                char[] lvar = stringToCharArray(charArrayToSting((char[]) localVars.get(m.group(1))));
                int idx = (int) interpretExpression(m.group(2), recurLimit);
                assert lvar != null;
                String cur = String.valueOf(lvar[idx]);

                String val = Operator.Operation(s, cur, Arrays.toString(right_val));

                assert val != null;
                lvar[idx] = val.toCharArray()[1];
                localVars.put(m.group(1), lvar);
                return val;
            } else {
                String cur = String.valueOf(localVars.get(m.group(1)));
                char[] val = stringToCharArray(Operator.Operation(s, cur, charArrayToSting(right_val)));
                localVars.put(m.group(1), val);
                return val;
            }
        }

        if (Character.isDigit(expr.charAt(0))) return Integer.parseInt(expr);

        if (expr.equals("break")) throw new JSBreak();
        else if (expr.equals("continue")) throw new JSContinue();

        Pattern var_pattern = Pattern.compile(String.format("(?!if|return|true|false)(%s)$", NAME_REGEX));
        Matcher var_m = var_pattern.matcher(expr);

        if (var_m.find() && var_m.matches()) {
            if (localVars.containsKey(var_m.group(1)))
                return localVars.get(var_m.group(1));
        }
        try {
            if (expr.equals("''") || expr.equals("\"\"")) return "\"\"";
            return new JSONObject(expr);
        } catch (JSONException e) {
        }
        Pattern m_p = Pattern.compile(String.format("(%s)\\[(.+)\\]$", NAME_REGEX));
        Matcher m = m_p.matcher(expr);
        if (m.find()) {
            char[] val = (char[]) localVars.get(m.group(1));
            val = stringToCharArray(charArrayToSting(val));
            int idx = (int) interpretExpression(m.group(2), recurLimit - 1);
            return val[idx];
        }

        for (String s : Operator.operators) {
            m_p = Pattern.compile(String.format("(.+?)\\%s(.+)", s));
            m = m_p.matcher(expr);
            if (!m.find()) continue;
            JSResultArray x = interpretStatement(m.group(1), recurLimit - 1);
            int i_x = Integer.parseInt(String.valueOf(x.res));
            if (x.abort) {
                try {
                    throw new Exception("Premature left-side return");
                } catch (Exception e) {
                }
            }
            JSResultArray y = interpretStatement(m.group(2), recurLimit - 1);
            int i_y = Integer.parseInt(String.valueOf(y.res));
            if (y.abort) {
                try {
                    throw new Exception("Premature right-side return");
                } catch (Exception e) {
                }
            }

            return Operator.Operation(s, i_x, i_y);
        }

        m_p = Pattern.compile(String.format("(%s)(?:\\.([^(]+)|\\[([^]]+)\\])\\s*", NAME_REGEX));
        m = m_p.matcher(expr);
        if (m.find()) {
            String variable = m.group(1);
            String member = removeQuotes(m.group(2));
            if (variable == null || variable.isEmpty()) variable = m.group(3);
            String arg_str = expr.substring(m.end());
            String remaining = null;
            if (arg_str.startsWith("(")) {
                ArrayList<String> list = separateAtParen(arg_str, ")");
                arg_str = list.get(0);
                remaining = list.get(1);
            } else {
                remaining = arg_str;
                arg_str = null;
            }
            if (remaining != null && !remaining.isEmpty()) {
                return interpretExpression(namedObject(evalMethod(member,variable,arg_str,recurLimit)) + remaining, recurLimit);
            } else {
                return evalMethod(member,variable,arg_str,recurLimit);
            }
        }

        m_p = Pattern.compile(String.format("^(%s)\\(([a-zA-Z0-9_$,]*)\\)$", NAME_REGEX));
        m = m_p.matcher(expr);
        if (m.find()) {
            String funcName = m.group(1);
            if (m.group(2).length() > 0) {
                for (String v : m.group(2).split(",")) {
                    if (Character.isDigit(v.charAt(0))) {
                        if (!functions.contains(funcName))
                            functions.put(funcName, extractFunction(funcName));
                        return functions.get(funcName).resf(argVal.toArray());
                    } else {
                    }
                }
            }
        } else {

        }

        return null;
    }

    Object evalMethod(String member, String variable, String argStr, int recurLimit) {
        Object obj;
        if (variable.equals("String")) obj = "";
        else if (localVars.containsKey(variable)) obj = localVars.get(variable);
        else{
            obj = extractObject(variable);//Line 380
            Hashtable<String, JSInterface> hashtable = (Hashtable<String, JSInterface>) obj;
            localVars.put(variable,hashtable);
            obj = localVars.get(variable);
        }

        if (argStr == null) {
            if (member.equals("length")){
                try{
                    return ((String) obj).length();
                }catch (Exception e){
                    return ((char[]) obj).length;
                }
            }
        }
        ArrayList<Object> argVals = new ArrayList<>();
        for (String v : separate(argStr, ",", -1)) {
            argVals.add(interpretExpression(v, recurLimit));
        }
        if (obj instanceof String) {
            if (member.equals("fromCharCode")) {
                StringBuilder val = new StringBuilder();
                for (Object o : argVals) {
                    val.append((char) ((int) o));
                }
                return val.toString();
            }
        }

        switch (member) {
            case "split":
                return UtilityClass.stringToCharArray((String) obj);
            case "join":
                return joinArray(stringToCharArray((String) argVals.get(0)), (char[]) obj);
            case "reverse":
                return reverse(stringToCharArray(charArrayToSting((char[]) obj)));
            case "slice":
                throw new RuntimeException("Not implemented");
            case "splice": {
                ArrayList<Character> objList = convertCharArrayToCharacterList((char[]) obj);
                argVals.add(objList.size());
                int index = Integer.parseInt(String.valueOf(argVals.get(0)));
                int howMany = Integer.parseInt(String.valueOf(argVals.get(1)));
                argVals.remove(argVals.size() - 1);
                if (index < 0) index += objList.size();
                ArrayList<Integer> addItems = new ArrayList<>();
                for (int i = 2; i < argVals.size(); i++) {
                    addItems.add(Integer.parseInt(String.valueOf(argVals.get(i))));
                }

                ArrayList<Object> res = new ArrayList<>();
                for (int i = index; i < Math.min(index + howMany, objList.size()); i++) {
                    res.add(objList.get(objList.size() - 1));
                    objList.remove(objList.size() - 1);
                }
                for (int i = 0; i < addItems.size(); i++) {
//                objList.add(index+i,); //Line 428
                }
                return convertObjectListToCharArray(res);
            }
            case "unshift":
                ArrayList<Character> tempObj = convertCharArrayToCharacterList((char[]) obj);
                for (char c : reverse(convertObjectListToCharArray(argVals))) {
                    tempObj.add(0, c);
                }
                return tempObj.toArray();
            case "pop":
                if (obj == null) return null;
                String v = charArrayToSting((char[]) obj);
                return stringToCharArray(v.substring(0, v.length() - 1));
            case "push": {
                ArrayList<Character> objList = convertCharArrayToCharacterList((char[]) obj);
                ArrayList<Object> tempList = new ArrayList<>(objList);
                for (Object o : argVals) {
                    tempList.add((char) o);
                }
                return convertObjectListToCharArray(tempList);
            }
            case "forEach": {
                JSInterface f = (JSInterface) argVals.get(0);
                ArrayList<Object> objects = new ArrayList<>();
                char[] objArray = ((char[]) obj);
                for (int i = 0; i < objArray.length; i++) {
                    objects.add((char[]) f.resf(new Object[]{objArray[i], i, objArray}));
                }
                return convertObjectListToCharArray(objects);
            }
            case "indexOf": {
                char idx = (char) argVals.get(0);
                int start = 0;
                try {
                    start = Integer.parseInt(String.valueOf(argVals.get(1)));
                } catch (ArrayIndexOutOfBoundsException e) {
                }

                int index = -1;
                char[] objArray = (char[]) obj;
                for (int i = start; i < objArray.length; i++) {
                    if (objArray[i] == idx) index = i;
                }
                return index;

            }
        }
        return ((JSInterface)((Hashtable<String,JSInterpreter>) obj).get(member)).resf(argVals.toArray());
    }

    private Hashtable<String, JSInterface> extractObject(String variable) {
        String FUNCTION_NAME = "(?:[a-zA-Z$0-9]+|\"[a-zA-Z$0-9]+\"|'[a-zA-Z$0-9]+')";
        Hashtable<String, JSInterface> obj = new Hashtable<>();
        Pattern obj_p = Pattern.compile(String.format("(?<!this\\.)%s\\s*=\\s*\\{\\s*((%s\\s*:\\s*function\\s*\\(.*?\\)\\s*\\{.*?\\}(?:,\\s*)?)*)\\}\\s*;", Matcher.quoteReplacement(variable), FUNCTION_NAME));
        //Pattern obj_p=Pattern.compile("(?<!this\\.)uw\\s*=\\s*\\{\\s*(((?:[a-zA-Z$0-9]+|\"[a-zA-Z$0-9]+\")\\s*:\\s*function\\s*\\(.*?\\)\\s*\\{.*?\\}(?:,\\s*)?)*)\\}\\s*;");
        Matcher obj_m = obj_p.matcher(code);
        if (obj_m.find()) {
            Pattern field_p = Pattern.compile(String.format("(%s)\\s*:\\s*function\\s*\\(([a-z,]+)\\)\\{([^}]+)\\}", FUNCTION_NAME));
            Matcher field_m = field_p.matcher(obj_m.group(1));
            while (field_m.find()) {
                String[] argnames = field_m.group(2).split(",");
                obj.put(UtilityClass.removeQuotes(field_m.group(1)), buildFunction(argnames, field_m.group(3)));
            }
            return obj;
        }
        return obj;
    }

    public static class JSFunctionCode {
        String[] args;
        String code;

        public JSFunctionCode(String[] args, String code) {
            this.args = args;
            this.code = code;
        }

        public JSFunctionCode(String code) {
            this.code = code;
        }
    }
}

class JSBreak extends IllegalArgumentException {}

class JSContinue extends IllegalArgumentException {}