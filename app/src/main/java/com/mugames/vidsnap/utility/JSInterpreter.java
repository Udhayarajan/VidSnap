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
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mugames.vidsnap.utility.UtilityClass.*;

/**
 * Used to decrypt the signature by Youtube and only used by {@link com.mugames.vidsnap.extractor.YouTube}
 */
public class JSInterpreter {

    public static final String NAME_REGEX="[a-zA-Z_$][a-zA-Z_$0-9]*";

    public String code;
    public Hashtable<String,Hashtable<String, JSInterface>> objects;
    public Hashtable<String, JSInterface> functions;
    Hashtable<String,Object> local_vars =  new Hashtable<>();




    public JSInterpreter(String code, Hashtable<String,Hashtable<String, JSInterface>> objects) {
        this.code = code;
        if(objects==null) this.objects = new Hashtable<>();
        else this.objects=objects;
        this.functions=new Hashtable<>();

    }

    public JSInterface Extract_Function(String functionName) {
        Pattern pattern = Pattern.compile(String.format("(?:function\\s+%s|[{;,]\\s*%s\\s*=\\s*function|var\\s+%s\\s*=\\s*function)\\s*\\(([^)]*)\\)\\s*\\{([^}]+)\\}"
                , functionName, functionName, functionName));
        Matcher func_m = pattern.matcher(code);
        if (func_m.find()) {
            String[] argnames = func_m.group(1).split(",");
            return Build_Function(argnames, func_m.group(2));
        }
        return null;
    }
    JSInterface Build_Function(String[] argnames, String function_code){

        return new JSInterface() {
            @Override
            public Object resf(Object[] values) {

                for (int i=0;i<argnames.length;i++){
                    local_vars.put(argnames[i],values[i]);
                }
                JSResultArray resultArray = new JSResultArray();
                for (String stmt : function_code.split(";")){
                    resultArray= Interpret_Statement(stmt,100);
                    if(resultArray.abort) break;
                }
                return resultArray.res;
            }
        };
    }



    public JSResultArray Interpret_Statement(String stmt,int recurLimit){
        String expr;
        JSResultArray resultArray=new JSResultArray();
        if(recurLimit<0){
            try {
                throw new Exception("Recursion limit reached");
            } catch (Exception e) {
            }
        }
        resultArray.abort=false;
        stmt=stmt.replaceAll("^\\s+", "");
        Pattern pattern=Pattern.compile("var\\s");
        Matcher stmt_m=pattern.matcher(stmt);
        if(stmt_m.find()) expr=stmt.substring(stmt_m.group(0).length());
        else{
            Pattern  return_p=Pattern.compile("return(?:\\s+|$)");
            Matcher return_m=return_p.matcher(stmt);
            if(return_m.find()){
                expr=stmt.substring(return_m.group(0).length());
                resultArray.abort=true;
            }
            else {
                expr=stmt;

            }
        }
        resultArray.res= Interpret_Expression(expr,recurLimit);
        return resultArray;
    }

    Object Interpret_Expression(String expr,int recurLimit){
        Object obj = new Hashtable<>();
        Hashtable<String, JSInterface> obj_m = new Hashtable<>();
        List<Object> arg_val = new ArrayList<>();
        expr = expr.trim();
        if(expr.isEmpty())
            return null;
        if(expr.startsWith("(")){
            int parens_Count=0;
            Pattern pattern =Pattern.compile("[()]");
            Matcher matcher=pattern.matcher(expr);
            while (matcher.find()){
                if(matcher.group(0).equals("(")) parens_Count++;
                else {
                    parens_Count-=1;
                    if(parens_Count==0){
                        String sub_expr=expr.substring(1,matcher.start());
                        String sub_res= (String) Interpret_Expression(sub_expr,recurLimit);
                        String remaining_expr=expr.substring(matcher.end()).trim();
                        if(remaining_expr.isEmpty()) return sub_res;
                        else expr= sub_res+remaining_expr;
                        break;
                    }
                    //else {/*wrong JS code*/}

                }
            }
        }
        for (String s : Operator.assign_operators){
            Pattern p=Pattern.compile(String.format("(%s)(?:\\[([^\\]]+?)\\])?\\s*\\%s(.*)$",NAME_REGEX,s));
            Matcher m = p.matcher(expr);
            if(!m.find()) continue;
            char[] right_val;
            Object o =  Interpret_Expression(m.group(3),recurLimit-1);
            try{
                right_val= stringToCharArray((String)o);
            } catch (Exception e) {
                try{
                    right_val= stringToCharArray(charArrayToSting((char[])o));

            } catch (Exception exception) {
                    right_val=new char[1];
                    right_val[0]=(char) o;
                }
            }

            if(m.group(2)!=null){
                char[] lvar= stringToCharArray(charArrayToSting((char[]) local_vars.get(m.group(1))));
                int idx= (int) Interpret_Expression(m.group(2),recurLimit);
                assert lvar != null;
                String cur=String.valueOf(lvar[idx]);

                String val=Operator.Operation(s,cur, Arrays.toString(right_val));

                assert val != null;
                lvar[idx] = val.toCharArray()[1];
                local_vars.put(m.group(1),lvar);
                return val;
            }
            else {
                String cur= String.valueOf(local_vars.get(m.group(1)));
                char[] val= stringToCharArray(Operator.Operation(s,cur, charArrayToSting(right_val)));
                local_vars.put(m.group(1),val);
                return val;
            }
        }

        if(Character.isDigit(expr.charAt(0))) return Integer.parseInt(expr);

        Pattern var_pattern=Pattern.compile(String.format("(?!if|return|true|false)(%s)$",NAME_REGEX));
        Matcher var_m=var_pattern.matcher(expr);

        if(var_m.find() && var_m.matches()) {
            if(local_vars.containsKey(var_m.group(1)))
                return local_vars.get(var_m.group(1));
        }
        try{
            if(expr.equals("''")||expr.equals("\"\""))return "\"\"";
            return new JSONObject(expr);
        } catch (JSONException e) {
        }
        Pattern m_p=Pattern.compile(String.format("(%s)\\[(.+)\\]$",NAME_REGEX));
        Matcher m=m_p.matcher(expr);
        if(m.find()){
            char[] val= (char[]) local_vars.get(m.group(1));
            val= stringToCharArray(charArrayToSting(val));
            int idx= (int) Interpret_Expression(m.group(2),recurLimit-1);
            return val[idx];
        }

        m_p=Pattern.compile(String.format("(%s)(?:\\.([^(]+)|\\[([^]]+)\\])\\s*(?:\\(+([^()]*)\\))?$",NAME_REGEX));
        m=m_p.matcher(expr);
        if(m.find()&& m.matches()){
            String variable=m.group(1);
            String member= removeQuotes(m.group(2));
            if (member.isEmpty()) member = removeQuotes(m.group(3));
            String arg_str=m.group(4);
            if(local_vars.containsKey(variable)) obj = local_vars.get(variable);
            else{
                if (!objects.contains(variable)){
                    objects.put(variable,Extract_Object(variable));
                }
                obj_m=objects.get(variable);
            }

            if(arg_str==null){
                if(member.equals("length")){
                    assert obj != null;
                    int x=obj_m.size();
                    if (x==0) x = stringToCharArray(charArrayToSting((char[])obj)).length;
                    return x;
                }
                return (obj_m.get(member));
            }
            assert expr.endsWith(")");
            if(arg_str.isEmpty()){
                arg_val=new ArrayList<>();
            }
            else {
                for (String v : arg_str.split(",")){
                    arg_val.add(Interpret_Expression(v,recurLimit));
                }
            }
            if(member.equals("split")){
                assert obj != null;
                return (obj);
            }
            if(member.equals("join")){
                char[] va= joinArray(stringToCharArray((String) arg_val.get(0)),(char[]) obj);
                local_vars.put(variable,va);
                return va;
            }
            if(member.equals("reverse")){
                char[] va= reverse(stringToCharArray( charArrayToSting((char[]) obj)));
                local_vars.put(variable,va);
                return va;
            }
            if(member.equals("slice")){
                Object va =((String)obj).substring(Integer.parseInt(String.valueOf((arg_val.get(0)))));
                local_vars.put(variable,va);
                return va;
            }
            if(member.equals("splice")){
                int index= Integer.parseInt((arg_val.get(0)+""));
                int howMany= Integer.parseInt((arg_val.get(1)+""));
                char[] t= (char[]) obj;
                String t_s= charArrayToSting(t);
                int isize=Math.min(index+howMany,t.length);
                char[] res=new char[isize];
                for(int i=index;i<isize;i++){
                    res[i]=t_s.charAt(i);
                    obj= popCharArray((char[]) obj,index);
                }
                local_vars.put(variable,obj);
                return res;
            }
            JSInterface anInterface=obj_m.get(member);
            return anInterface.resf(arg_val.toArray());
        }

        for (String s:Operator.operators){
            m_p=Pattern.compile(String.format("(.+?)\\%s(.+)",s));
            m=m_p.matcher(expr);
            if(!m.find()) continue;
            JSResultArray x = Interpret_Statement(m.group(1),recurLimit-1);
            int i_x= Integer.parseInt(String.valueOf(x.res));
            if(x.abort) {
                try {
                    throw new Exception("Premature left-side return");
                } catch (Exception e) {
                }
            }
            JSResultArray y=Interpret_Statement(m.group(2),recurLimit-1);
            int i_y= Integer.parseInt(String.valueOf(y.res));
            if(y.abort){
                try{
                    throw new Exception("Premature right-side return");
                } catch (Exception e) {
                }
            }

            return Operator.Operation(s,i_x,i_y);
        }

        m_p=Pattern.compile(String.format("^(%s)\\(([a-zA-Z0-9_$,]*)\\)$",NAME_REGEX));
        m=m_p.matcher(expr);
        if(m.find()) {
            String funcName = m.group(1);
            if (m.group(2).length() > 0) {
                for (String v : m.group(2).split(",")) {
                    if (Character.isDigit(v.charAt(0))) {
                        if(!functions.contains(funcName))
                            functions.put(funcName,Extract_Function(funcName));
                        return functions.get(funcName).resf(arg_val.toArray());
                    }
                    else {}
                }
            }
        }else {

        }

        return null;
    }

    private Hashtable<String, JSInterface> Extract_Object(String variable) {
        String FUNCTION_NAME="(?:[a-zA-Z$0-9]+|\"[a-zA-Z$0-9]+\"|'[a-zA-Z$0-9]+')";
        Hashtable<String, JSInterface> obj = new Hashtable<>();
        Pattern obj_p=Pattern.compile(String.format("(?<!this\\.)%s\\s*=\\s*\\{\\s*((%s\\s*:\\s*function\\s*\\(.*?\\)\\s*\\{.*?\\}(?:,\\s*)?)*)\\}\\s*;",variable,FUNCTION_NAME));
        //Pattern obj_p=Pattern.compile("(?<!this\\.)uw\\s*=\\s*\\{\\s*(((?:[a-zA-Z$0-9]+|\"[a-zA-Z$0-9]+\")\\s*:\\s*function\\s*\\(.*?\\)\\s*\\{.*?\\}(?:,\\s*)?)*)\\}\\s*;");
        Matcher obj_m=obj_p.matcher(code);
        if(obj_m.find()) {
            Pattern field_p = Pattern.compile(String.format("(%s)\\s*:\\s*function\\s*\\(([a-z,]+)\\)\\{([^}]+)\\}", FUNCTION_NAME));
            Matcher field_m = field_p.matcher(obj_m.group(1));
            while (field_m.find()) {
                String[] argnames = field_m.group(2).split(",");
                obj.put(UtilityClass.removeQuotes(field_m.group(1)), Build_Function(argnames, field_m.group(3)));
            }
            return obj;
        }
        return obj;
    }
}