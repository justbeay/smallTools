package com.tool.utils;

import org.junit.Test;

import java.lang.reflect.Method;

/**
 * Created by Administrator on 2017/12/15.
 */
public class ReflectUtilTest {

    @Test
    public void testInvoke() throws Exception{
        Method method = ReflectUtil.getDeclaredMethodRecurive(B.class, "test");
        method.invoke(new B(), ReflectUtil.formatInvokeParams(method));

        method = ReflectUtil.getDeclaredMethod(B.class, "test3", String.class, String[].class);
        method.invoke(new B(), ReflectUtil.formatInvokeParams(method, "testName", "aaa", "bbb"));
        method.invoke(new B(), ReflectUtil.formatInvokeParams(method, "testName", "aaa"));
        method.invoke(new B(), ReflectUtil.formatInvokeParams(method, "testName"));

        method = ReflectUtil.getDeclaredMethod(B.class, "test4", String.class);
        method.invoke(new B(), ReflectUtil.formatInvokeParams(method, "hello"));
        // 入参为null的情况
        method.invoke(new B(), ReflectUtil.formatInvokeParams(method, new Object[]{ null }));
    }

    @Test
    public void testInvokeMatchedMethod(){
        B obj = new B();
        ReflectUtil.invokeMatchedMethodForce(obj, "test2");
        ReflectUtil.invokeMatchedMethod(obj, "test3", "testName");
        ReflectUtil.invokeMatchedMethod(obj, "test3", "testName", "aaa");
        ReflectUtil.invokeMatchedMethod(obj, "test3", "testName", null);
        ReflectUtil.invokeMatchedMethod(obj, "test3", "testName", null, null);
        ReflectUtil.invokeMatchedMethod(obj, "test4", null);
    }

    class A{
        public void test(){
            System.out.println("test runs here...");
            System.out.println(this.getClass().getSimpleName());
        }
    }
    class B extends A {
        private void test2(){
            System.out.println("test2 runs here...");
        }

        public void test3(String name, String... params){
            System.out.println("test3 with name:"+name+" runs here...");
            for(int i=0; i<params.length; i++){
                System.out.println(i+": "+params[i]);
            }
        }

        public void test4(String param){
            System.out.println("test4 runs here...");
        }
    }
}
