package com.kevin.pdcu.verSync;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author TX
 * @date 2021/3/17 15:16
 */
public class TestSyncForConsumerAndProducer {

    private static Object lock = new Object();
    private static DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static String getDateTimeStr(){
        return df.format(LocalDateTime.now(ZoneId.of("UTC")));
    }
    private static void log(String msg){
        System.out.println(getDateTimeStr() + "|" + Thread.currentThread().getName() + ": " + msg);
    }

    private static int value = 0;
    private static int max_queue_len=3;

    public static void main(String[] args) {
        //最后一条打印应该类似：2021-03-17 03:31:10|tB-3: after sub: 0: 99，
        // 即循环序号是99，且执行的是sub操作，执行后value的值为0
        for (int i = 0; i < 5; i++) {
            //创建5个Add线程，tA-0到tA-4，每个线程获得锁，将value加1，再释放锁，
            // value等于max_queue_len时await，进入cdA等待队列等待Sub线程将value减小后唤醒
            new Thread(() -> {
                for(int j=0; j<100; j++) {
                    log("to get lock: " + j);
                    synchronized (lock) {
                        try {
                            log("have got lock: " + j);
//                        Thread.sleep(1000);
                            //一定要用while判断是否还没有达到继续执行的条件，
                            // 因为被唤醒后，可能当本线程执行时，其他线程已经执行导致条件又不满足了，此时需要继续wait
                            while (value >= max_queue_len) {
                                log("wait() start: " + j);
                                lock.wait();
                                log("wait() end: " + j);
                            }
                            value++;
                            log("after add: " + value + ": " + j);
                            if (value > 0) {
                                //注意，这里不管是Add线程，还是Sub线程，wait后都是在同一个阻塞队列，所以要使用notifyAll，否则可能又唤醒了一个Add线程
                                //假设这时正好其他线程都在阻塞队列，且value已经等于max_queue_len了，这时被唤醒的Add队列又会wait，这时就所有线程都阻塞了
                                //程序僵死，当然，使用notifyAll会有一定性能损耗，因为相当于把所有的阻塞队列的线程都唤醒添加到同步队列了，这里没有ReentrainLock的
                                //Condition的多个条件阻塞队列
                                lock.notifyAll();
                            }
                        } catch (Exception ex) {
                            log(ex.toString());
                            ex.printStackTrace();
                        }
                    }
                }
            }, "tA-" + i).start();

            //创建5个Sub线程，tB-0到tB-4，每个线程获得锁，将value减1，再释放锁，
            // value等于0时await，进入cdB等待队列等待Add线程将value增加后唤醒
            new Thread(() -> {
                for(int j=0; j<100; j++) {
                    log("to get lock: " + j);
                    synchronized (lock) {
                        try {
                            log("have got lock: " + j);
//                        Thread.sleep(1000);
                            //一定要用while判断是否还没有达到继续执行的条件，
                            // 因为被唤醒后，可能当本线程执行时，其他线程已经执行导致条件又不满足了，此时需要继续await
                            while (value <= 0) {
                                log("wait() start: " + j);
                                lock.wait();
                                log("wait() end: " + j);
                            }
                            value--;
                            log("after sub: " + value + ": " + j);
                            if (value < max_queue_len) {
                                //注意，这里不管是Add线程，还是Sub线程，wait后都是在同一个阻塞队列，所以要使用notifyAll，否则可能又唤醒了一个Sub线程
                                //假设这时正好其他线程都在阻塞队列，且value已经等于0了，这时被唤醒的Sub队列又会wait，这时就所有线程都阻塞了
                                //程序僵死，当然，使用notifyAll会有一定性能损耗，因为相当于把所有的阻塞队列的线程都唤醒添加到同步队列了，这里没有ReentrainLock的
                                //Condition的多个条件阻塞队列
                                lock.notifyAll();
                            }
                        } catch (Exception ex) {
                            log(ex.toString());
                            ex.printStackTrace();
                        }
                    }
                }
            }, "tB-" + i).start();
        }
    }
}
