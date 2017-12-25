package com.tool.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Administrator on 2017/12/15.
 */
public class ReflectUtil {

    public static Method getDeclaredMethod(Class<?> clazz, String method, Class<?>... paramTypes){
        try {
            return clazz.getDeclaredMethod(method, paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static Method getDeclaredMethodRecurive(Class<?> clazz, String method, Class<?>... paramTypes){
        Method retMethod = null;
        Class<?> curClazz = clazz;
        do{
            retMethod = getDeclaredMethod(curClazz, method, paramTypes);
            if(retMethod != null){
                break;
            }
            curClazz = curClazz.getSuperclass();
        } while(curClazz != null);
        return retMethod;
    }

    public static Method[] getDeclaredMethods(Class<?> clazz, String method){
        Method[] methodList = clazz.getDeclaredMethods();
        if(methodList.length == 0) return new Method[]{};

        List<Method> retMethodList = new ArrayList<Method>();
        for(Method tmpMethod : methodList){
            if(tmpMethod.getName().equals(method)){
                retMethodList.add(tmpMethod);
            }
        }

        Method[] retMethods = new Method[retMethodList.size()];
        return retMethodList.toArray(retMethods);
    }

    public static Method[] getDeclaredMethodsRecurive(Class<?> clazz, String method){
        if(clazz == null) throw new NullPointerException();
        List<Method> retMethodList = new ArrayList<Method>();
        Class<?> curClazz = clazz;
        do{
            Method[] curMethods = getDeclaredMethods(curClazz, method);
            for(int i=0; i<curMethods.length; i++){
                retMethodList.add(curMethods[i]);
            }
            curClazz = curClazz.getSuperclass();
        }while(curClazz != null);

        Method[] retMethods = new Method[retMethodList.size()];
        return retMethodList.toArray(retMethods);
    }

    /**
     * 判断rightClazz是否为leftClazz的子类或相等
     * @param leftClazz
     * @param rightClazz
     * @return
     */
    private static boolean isClassEqualOrSub(Class<?> leftClazz, Class<?> rightClazz){
        if(leftClazz.equals(rightClazz)) return true;
        else if(leftClazz.isArray()) return false;

        boolean equal = false;
        Class<?> curClazz = leftClazz.getSuperclass();
        List<Class<?>> curInterfaces = new ArrayList<Class<?>>();
        if(rightClazz.isInterface()){
            curInterfaces.addAll(Arrays.asList(leftClazz.getInterfaces()));
        }
        while(!equal && (curClazz != null || !curInterfaces.isEmpty())){
            List<Class<?>> subInterfaces = new ArrayList<Class<?>>();
            if(rightClazz.isInterface()){
                for(Class<?> tmpInterface : curInterfaces){
                    if(curClazz.equals(tmpInterface)){
                        equal = true;
                        break;
                    }else{
                        subInterfaces.addAll(Arrays.asList(tmpInterface.getInterfaces()));
                    }
                }
                subInterfaces.addAll(Arrays.asList(curClazz.getInterfaces()));
            }else{
                equal = curClazz.equals(rightClazz);
            }
            curClazz = curClazz == null ? null : curClazz.getSuperclass();
            curInterfaces = subInterfaces;
        }
        return equal;
    }

    public static Method findMethodByParamType(Method[] reflectMethods, Class<?>... paramTypes){
        Method reflectMethod = null;
        if(reflectMethods != null && reflectMethods.length > 0){
            for(int i=0; i<reflectMethods.length; i++){
                Class<?>[] curParamTypes = reflectMethods[i].getParameterTypes();
                boolean typeEqual = true;
                int curParamTypeIndex = 0;
                int paramTypeIndex = 0;
                if(curParamTypes.length > 0) {
                    while (curParamTypeIndex < curParamTypes.length) {
                        Class<?> curParamType = curParamTypes[curParamTypeIndex];
                        if(paramTypeIndex >= paramTypes.length){  // 方法对应参数未指定情况
                            if(!curParamType.isArray()){
                                typeEqual = false;
                                break;
                            }else if(curParamTypeIndex == curParamTypes.length - 1){  // 匹配最后一个参数类型且为数组类型子类型
                                break;
                            }
                        }else if (isClassEqualOrSub(curParamType, paramTypes[paramTypeIndex])) {
                            curParamTypeIndex++;
                        } else if (!curParamType.isArray() || !isClassEqualOrSub(curParamType.getComponentType(), paramTypes[paramTypeIndex])) {
                            typeEqual = false;  // 当前参数类型不匹配，无需继续比较后续参数类型
                            break;
                        }
                        paramTypeIndex++;
                    }
                }
                if(typeEqual && paramTypeIndex == paramTypes.length && curParamTypeIndex >= curParamTypes.length - 1){  // 找到匹配的方法
                    reflectMethod = reflectMethods[i];
                    break;
                }
            }
        }
        return reflectMethod;
    }

    public static Object[] formatInvokeParams(Method reflectMethod, Object... params){
        if(reflectMethod == null) throw new NullPointerException();
        Class<?>[] curParamTypes = reflectMethod.getParameterTypes();
        Object[] retResult = new Object[curParamTypes.length];
        if(params == null && curParamTypes.length > 0){  // 标准化参数，当做方法传null入参处理
            params = new Object[]{ null };
        }
        int paramIndex = 0;
        int curParamIypeIndex = 0;
        while(curParamIypeIndex < retResult.length && (paramIndex < params.length || retResult[curParamIypeIndex] == null)){
            Class<?> curParamType = curParamTypes[curParamIypeIndex];
            if(paramIndex >= params.length && retResult[curParamIypeIndex] == null){  // 方法对应参数未指定情况
                if(curParamType.isArray()){
                    retResult[curParamIypeIndex ++] = Array.newInstance(curParamType.getComponentType(), 0);
                    continue;
                }else{
                    throw new RuntimeException("param type not match");
                }
            }else if (params[paramIndex] == null && !curParamType.isArray() ||
                    params[paramIndex] != null && isClassEqualOrSub(curParamType, params[paramIndex].getClass())) {  //类型相同或非数组类型且为Null
                retResult[curParamIypeIndex] = params[paramIndex];
                curParamIypeIndex++;
            }else if (curParamType.isArray() && (params[paramIndex] == null ||
                    isClassEqualOrSub(curParamType.getComponentType(), params[paramIndex].getClass()))) {  // 数组元素类型，且入参为null或子元素类型
                // 数组参数size+1，保存对应参数至末尾
                int oldSize = retResult[curParamIypeIndex] == null ? 0 : Array.getLength(retResult[curParamIypeIndex]);
                if(oldSize > 0){
                    Object oldParams = retResult[curParamIypeIndex];
                    retResult[curParamIypeIndex] = Array.newInstance(curParamType.getComponentType(), oldSize + 1);
                    for(int j=0; j<oldSize; j++){
                        Array.set(retResult[curParamIypeIndex], j, Array.get(oldParams, j));
                    }
                }else{
                    retResult[curParamIypeIndex] = Array.newInstance(curParamType.getComponentType(), 1);
                }
                Array.set(retResult[curParamIypeIndex], oldSize, params[paramIndex]);
            }else{  // 当前参数类型不匹配，无需继续比较后续参数类型
                throw new RuntimeException("param type not match");
            }
            paramIndex++;
        }
        return retResult;
    }

    private static Object invokeMatchedMethod(Object obj, String method, boolean force, Object... params){
        Class<?>[] paramTypes = params == null ? new Class<?>[1] : new Class<?>[params.length];
        if(params == null){
            paramTypes[0] = Object.class;
        }else{
            for(int i=0; i<params.length; i++){
                paramTypes[i] = params[i] == null ? Object.class : params[i].getClass();
            }
        }
        Class<?> clazz = obj.getClass();
        Method[] reflectMethods = getDeclaredMethodsRecurive(clazz, method);
        Method reflectMethod = findMethodByParamType(reflectMethods, paramTypes);
        if(reflectMethod == null){
            throw new RuntimeException("no such method:" + method + " for class:" + clazz.getName() + " with given params");
        }
        Object[] invokeParams = formatInvokeParams(reflectMethod, params);
        if(force){
            reflectMethod.setAccessible(true);
        }
        try {
            return reflectMethod.invoke(obj, invokeParams);
        } catch (Exception e) {
            throw new RuntimeException("method:"+method+" for class:"+clazz.getName()+" invoke error", e);
        }
    }

    public static Object invokeMatchedMethod(Object obj, String method, Object... params){
        return invokeMatchedMethod(obj, method, false, params);
    }

    public static Object invokeMatchedMethodForce(Object obj, String method, Object... params){
        return invokeMatchedMethod(obj, method, true, params);
    }
}
