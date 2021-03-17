package com.kevin.pdcu.verReentrantLock;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author TX
 * @date 2021/3/16 15:52
 */
class ShareData {
    private int number = 0;
    private Lock lock = new ReentrantLock();
    private Condition condition_e0 = lock.newCondition();
    private Condition condition_n0 = lock.newCondition();

    public void increment() {
        lock.lock();
        try {
            while (number != 0) {
                condition_e0.await();
            }
            number++;
            System.out.println(Thread.currentThread().getName() + "\t" + number);
            if(number!=0) {
                condition_n0.signalAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void decrement() {
        lock.lock();
        try {
            while (number == 0) {
                condition_n0.await();
            }
            number--;
            System.out.println(Thread.currentThread().getName() + "\t" + number);
            if(number == 0) {
                condition_n0.signalAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}

public class MyProdConsumerReentrantLockDemo {
    public static void main(String[] args) {
        ShareData shareData = new ShareData();
        new Thread(() -> {
            for (int i = 0;i < 30;i++) {
                shareData.increment();
            }
        },"A").start();

        new Thread(() -> {
            for (int i = 0;i < 30;i++) {
                shareData.decrement();
            }
        },"B").start();
    }
}
