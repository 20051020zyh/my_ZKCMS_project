package com.heima.big_event;

import org.junit.jupiter.api.Test;

public class ThreadLocalTest {
    @Test
    public void testThreaLocalSetAndGet(){
        //创建一个ThreadLocal对象
        ThreadLocal tl = new ThreadLocal();

        //开启两个线程
        new Thread(() ->{
            tl.set("萧炎");
            System.out.println(Thread.currentThread().getName() + ":" + tl.get());
            System.out.println(Thread.currentThread().getName() + ":" + tl.get());
            System.out.println(Thread.currentThread().getName() + ":" + tl.get());
        } ,"蓝色").start();

        new Thread(() ->{
            tl.set("药尘");
            System.out.println(Thread.currentThread().getName() + ":" + tl.get());
            System.out.println(Thread.currentThread().getName() + ":" + tl.get());
            System.out.println(Thread.currentThread().getName() + ":" + tl.get());
        } ,"绿色").start();
    }
}
