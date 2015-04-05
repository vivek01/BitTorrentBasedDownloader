package com.p2p.peercds.common;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Test
{
   public static void main(String[] args) throws Exception
   {
      Shared s = new Shared();
      new Consumer(s).start();
      Thread.sleep(3000);
      new Producer(s).start();
      
   }
}

class Shared
{
   // Fields c and available are volatile so that writes to them are visible to 
   // the various threads. Fields lock and condition are final so that they're
   // initial values are visible to the various threads. (The Java memory model 
   // promises that, after a final field has been initialized, any thread will 
   // see the same [correct] value.)

   private volatile char c;
   private volatile boolean available;
   private final ReentrantLock lock;
   private final Condition condition;

   Shared()
   {
      c = '\u0000';
      available = false;
      lock = new ReentrantLock();
      condition = lock.newCondition();
   }

   ReentrantLock getLock()
   {
      return lock;
   }

   char getSharedChar()
   {
      lock.lock();
      try
      {
         while (!available)
            try
            {
               condition.await();
               System.out.println("hold count "+lock.getHoldCount());
            }
            catch (InterruptedException ie)
            {
               ie.printStackTrace();
            }
         available = false;
         condition.signal();
      }
      finally
      {
         lock.unlock();
         return c;
      }
   }

   void setSharedChar(char c)
   {
      lock.lock();
      try
      {
         while (available)
            try
            {
               condition.await();
            }
            catch (InterruptedException ie)
            {
               ie.printStackTrace();
            }
         this.c = c;
         available = true;
         condition.signal();
      }
      finally
      {
         lock.unlock();
      }
   }
}

class Producer extends Thread
{
   // l is final because it's initialized on the main thread and accessed on the
   // producer thread.

   private final ReentrantLock l;

   // s is final because it's initialized on the main thread and accessed on the
   // producer thread.

   private final Shared s;
   
   Producer(Shared s)
   {
      this.s = s;
      l = s.getLock();
   }

   @Override
   public void run()
   {
      for (char ch = 'A'; ch <= 'Z'; ch++)
      {
         l.lock();
         System.out.println("hold count "+l.getHoldCount());
         s.setSharedChar(ch);
         System.out.println(ch + " produced by producer.");
         l.unlock();
      }
   }
}
class Consumer extends Thread
{
   // l is final because it's initialized on the main thread and accessed on the
   // consumer thread.

   private final Lock l;

   // s is final because it's initialized on the main thread and accessed on the
   // consumer thread.

   private final Shared s;

   Consumer(Shared s)
   {
      this.s = s;
      l = s.getLock();
   }

   @Override
   public void run()
   {
      char ch;
      do
      {
         l.lock();
         ch = s.getSharedChar();
         System.out.println(ch + " consumed by consumer.");
         l.unlock();
      }
      while (ch != 'Z');
   }
}