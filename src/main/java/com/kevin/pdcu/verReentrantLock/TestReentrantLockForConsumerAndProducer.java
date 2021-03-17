package com.kevin.pdcu.verReentrantLock;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author TX
 * @date 2021/3/16 19:51
 */
public class TestReentrantLockForConsumerAndProducer {

    private static Lock lock = new ReentrantLock();
    private static Condition cdA = lock.newCondition();
    private static Condition cdB = lock.newCondition();
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
                    lock.lock();
                    try {
                        log("have got lock: " + j);
//                        Thread.sleep(1000);
                        //一定要用while判断是否还没有达到继续执行的条件，
                        // 因为被唤醒后，可能当本线程执行时，其他线程已经执行导致条件又不满足了，此时需要继续await
                        while (value >= max_queue_len) {
                            log("cdA.await() start: " + j);
                            cdA.await();
                            log("cdA.await() end: " + j);
                        }
                        value++;
                        log("after add: " + value + ": " + j);
                        if (value > 0) {
//                            cdB.signalAll();
                            cdB.signal();
                        }
                    } catch (Exception ex) {
                        log(ex.toString());
                        ex.printStackTrace();
                    } finally {
                        lock.unlock();
                    }
                }
            }, "tA-" + i).start();

            //创建5个Sub线程，tB-0到tB-4，每个线程获得锁，将value减1，再释放锁，
            // value等于0时await，进入cdB等待队列等待Add线程将value增加后唤醒
            new Thread(() -> {
                for(int j=0; j<100; j++) {
                    log("to get lock: " + j);
                    lock.lock();
                    try {
                        log("have got lock: " + j);
//                        Thread.sleep(1000);
                        //一定要用while判断是否还没有达到继续执行的条件，
                        // 因为被唤醒后，可能当本线程执行时，其他线程已经执行导致条件又不满足了，此时需要继续await
                        while (value <= 0) {
                            log("cdB.await() start: " + j);
                            cdB.await();
                            log("cdB.await() end: " + j);
                        }
                        value--;
                        log("after sub: " + value + ": " + j);
                        if (value < max_queue_len) {
//                            cdA.signalAll();
                            cdA.signal();
                        }
                    } catch (Exception ex) {
                        log(ex.toString());
                        ex.printStackTrace();
                    } finally {
                        lock.unlock();
                    }
                }
            }, "tB-" + i).start();
        }
    }
}
