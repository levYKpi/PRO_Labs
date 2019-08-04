import java.util.concurrent.locks.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Semaphore;


class CommonResourceCR1
{
    public static final int MaxBufSize = 19;
    public static final int MinBufSize = 0;
    int buf[] = new int[MaxBufSize+1];
    int ind = 0;
    boolean IsEmpty = ind  == MinBufSize;
    boolean IsFull = ind == MaxBufSize;
    int full = 0;
    int empty = 0;
    boolean stop = false;

    synchronized void consume(int i)
    {
        while (IsEmpty)
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                System.out.println("Interrupt consume thread"+i);
                return;
            }
        System.out.println("consume in thread"+i);
        System.out.println("--- buf[" + ind + "] = " + buf[ind]);
        buf[ind] = 0;
        System.out.println("--- ind: " + ind-- + " => " + ind);
        System.out.println();

        IsEmpty = ind == MinBufSize;
        if(IsEmpty) empty++;
        IsFull = false;
        notify();
    }


    synchronized void produce (int i)
    {
        while (IsFull)

            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                System.out.println("produce_interrupt");
            }
        System.out.println("produce in thread"+i);
        System.out.println("+++ ind: " + ind++ +" => " + ind);
        buf[ind] = ind;
        System.out.println("+++ buf[" + ind + "] = " + buf[ind]);
        System.out.println();

        IsFull = ind == MaxBufSize;
        if(IsFull) full++;
        IsEmpty = false;

        notify();
    }
}

class CommonResourceCR2
{
    public static int a = 0;
    public static double b = 0;
    public static float c = 0;
    public static byte d = 0;
    public static short e = 0;
    public static long f = 0;
    public static char h = 0;
    public static boolean k = true;

    public static ReentrantLock mutex = new ReentrantLock();
    public static void Modif_var(){
        a++;b++;c++;d++;e++;f++;h++;k=!k;
        System.out.println(a+" "+b+" "+c+" "+d+" "+e+" "+f+" "+h+" "+k);
    }
}


class thread1 implements Runnable
{
    Thread t;
    CyclicBarrier bar;
    Semaphore sem1, sem2;
    CommonResourceCR1 CR1;
    thread1(CyclicBarrier bar, Semaphore sem1, Semaphore sem2, CommonResourceCR1 CR1){
        this.bar = bar;
        this.sem1 = sem1;
        this.sem2 = sem2;
        this.CR1 = CR1;
        t = new Thread(this, "thread1");
        t.start();
    }
    public void run(){
        while (true)
        {
            if(CR1.stop == true) break;
            System.out.println("before barrier thread1");
            try{
                bar.await();
            }
            catch(BrokenBarrierException e){
                System.out.println("BrokenBarrier thread1");
                break;
            }
            catch(InterruptedException e) {
                System.out.println("Interrupt thread1");
                break;
            }System.out.println("after barrier thread1");

            CommonResourceCR2.mutex.lock();
            System.out.println("modification thread1");
            CommonResourceCR2.Modif_var();
            CommonResourceCR2.mutex.unlock();

            System.out.println("before sem1-2 thread1");
            System.out.println("before sem 1 post thread1");
            sem1.release();System.out.println("before sem2 wait thread1");
            try{
                sem2.acquire();
            }
            catch (InterruptedException e){
                System.out.println("Interrupt thread1");
                break;
            }
        }
        //threads.t2.t.interrupt();
        //threads.t5.t.interrupt();
        System.out.println("thread1 -- break");
    }
}

class thread2 implements Runnable
{
    Thread t;
    CyclicBarrier bar;
    CommonResourceCR1 CR1;
    thread2(CyclicBarrier bar, CommonResourceCR1 CR1){
        this.bar = bar;
        this.CR1 = CR1;
        t = new Thread(this, "thread2");
        t.start();
    }
    public void run(){
        while (true)
        {
            if(CR1.stop == true) break;
            System.out.println("before barrier thread2");
            try{
                bar.await();
            }
            catch(BrokenBarrierException e){
                System.out.println("BrokenBarrier thread2");
                break;
            }
            catch(InterruptedException e) {
                System.out.println("Interrupt thread2");
                break;
            }System.out.println("after barrier thread2");



            CR1.consume(2);
        }
        //threads.t1.t.interrupt();
        //threads.t5.t.interrupt();
        System.out.println("thread2 -- break");
    }
}

class thread3 implements Runnable
{
    Thread t;
    CommonResourceCR1 CR1;
    CyclicBarrier bar;
    thread3(CommonResourceCR1 CR1,CyclicBarrier bar){
        this.CR1 = CR1;
        this.bar = bar;
        t = new Thread(this, "thread4");
        t.start();
    }
    public void run(){
        while (true)
        {
            if(CR1.full >= 2 && CR1.empty >= 1 && CR1.IsFull) break;
            CR1.produce(3);
        }CR1.stop = true;bar.reset();System.out.println("Barrier reset thread3");
        threads.t1.t.interrupt();System.out.println("interrupt t1 in thread3");
        threads.t2.t.interrupt();System.out.println("interrupt t2 in thread3");
        threads.t5.t.interrupt();System.out.println("interrupt t5 in thread3");
        System.out.println("thread3 -- break");
    }
}

class thread4 implements Runnable
{
    Thread t;
    Semaphore sem1, sem2;
    CommonResourceCR1 CR1;
    thread4(Semaphore sem1, Semaphore sem2, CommonResourceCR1 CR1){
        this.sem1 = sem1;
        this.sem2 = sem2;
        this.CR1 = CR1;
        t = new Thread(this, "thread4");
        t.start();
    }
    public void run(){
        while (true)
        {
            System.out.println("before sem1-2 thread4");
            System.out.println("before sem2 post thread4");
            sem2.release();
            System.out.println("before sem1 wait thread4");
            try{
                sem1.acquire();
            }
            catch (InterruptedException e){
                System.out.println("Interrupt thread4");
            }
            if(CR1.stop) break;
            CommonResourceCR2.mutex.lock();
            System.out.println("modification thread4");
            CommonResourceCR2.Modif_var();
            CommonResourceCR2.mutex.unlock();
        }
        //threads.t1.t.interrupt();
        //threads.t2.t.interrupt();
        //threads.t5.t.interrupt();
        System.out.println("thread4 -- break");
    }
}

class thread5 implements Runnable
{
    Thread t;
    CyclicBarrier bar;
    CommonResourceCR1 CR1;
    boolean f = true;
    thread5(CyclicBarrier bar, CommonResourceCR1 CR1){
        this.bar = bar;
        this.CR1 = CR1;
        t = new Thread(this, "thread5");
        t.start();
    }
    public void run(){
        while (f)
        {
            if(CR1.stop == true) break;
            System.out.println("before barrier thread5");
            try{
                bar.await();
            }
            catch(BrokenBarrierException e){
                System.out.println("BrokenBarrier thread5");
                break;
            }
            catch(InterruptedException e) {
                System.out.println("Interrupt thread5");
                break;
            }System.out.println("after barrier thread5");

            CR1.consume(5);

            CommonResourceCR2.mutex.lock();
            System.out.println("modification thread5");
            CommonResourceCR2.Modif_var();
            CommonResourceCR2.mutex.unlock();
        }
        //threads.t1.t.interrupt();
        //threads.t2.t.interrupt();
        System.out.println("thread5 -- break");
    }
}

class thread6 implements Runnable
{
    Thread t;
    CommonResourceCR1 CR1;
    Semaphore sem1; Semaphore sem2;
    thread6(CommonResourceCR1 CR1,Semaphore sem1, Semaphore sem2){
        this.CR1 = CR1;
        this.sem1 = sem1;this.sem2 = sem2;
        t = new Thread(this, "thread4");
        t.start();
    }
    public void run(){
        while (true)
        {
            if(CR1.stop && CR1.IsEmpty) break;
            CR1.consume(6);
        }sem1.release(); sem2.release();
        //threads.t1.t.interrupt();
        //threads.t2.t.interrupt();
        //threads.t5.t.interrupt();
        System.out.println("thread6 -- break");
    }
}

class threads{
    public static thread1 t1;
    public static thread2 t2;
    public static thread3 t3;
    public static thread4 t4;
    public static thread5 t5;
    public static thread6 t6;
}

class Main
{
    public static void main (String args[])
    {
        CyclicBarrier br = new CyclicBarrier(3);
        CommonResourceCR1 CR1 = new CommonResourceCR1();
        Semaphore sem1 = new Semaphore(0, true);
        Semaphore sem2 = new Semaphore(0, true);
        threads.t1 = new thread1(br, sem1, sem2, CR1);
        threads.t2 = new thread2(br,CR1);
        threads.t3 = new thread3(CR1,br);
        threads.t4 = new thread4(sem1, sem2, CR1);
        threads.t5 = new thread5(br,CR1);
        threads.t6 = new thread6(CR1,sem1,sem2);
        try
        {
            threads.t1.t.join();
            threads.t2.t.join();
            threads.t3.t.join();
            threads.t4.t.join();
            threads.t5.t.join();
            threads.t6.t.join();
        }catch(InterruptedException e){
            System.out.println("Main Interrupted\n");
        }
        System.out.println("Main stop\n");
    }
}
