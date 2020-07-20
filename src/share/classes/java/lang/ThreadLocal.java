/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang;
import java.lang.ref.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * This class provides thread-local variables.  These variables differ from
 * their normal counterparts in that each thread that accesses one (via its
 * {@code get} or {@code set} method) has its own, independently initialized
 * copy of the variable.  {@code ThreadLocal} instances are typically private
 * static fields in classes that wish to associate state with a thread (e.g.,
 * a user ID or Transaction ID).
 *
 * <p>For example, the class below generates unique identifiers local to each
 * thread.
 * A thread's id is assigned the first time it invokes {@code ThreadId.get()}
 * and remains unchanged on subsequent calls.
 * <pre>
 * import java.util.concurrent.atomic.AtomicInteger;
 *
 * public class ThreadId {
 *     // Atomic integer containing the next thread ID to be assigned
 *     private static final AtomicInteger nextId = new AtomicInteger(0);
 *
 *     // Thread local variable containing each thread's ID
 *     private static final ThreadLocal&lt;Integer&gt; threadId =
 *         new ThreadLocal&lt;Integer&gt;() {
 *             &#64;Override protected Integer initialValue() {
 *                 return nextId.getAndIncrement();
 *         }
 *     };
 *
 *     // Returns the current thread's unique ID, assigning it if necessary
 *     public static int get() {
 *         return threadId.get();
 *     }
 * }
 * </pre>
 * <p>Each thread holds an implicit reference to its copy of a thread-local
 * variable as long as the thread is alive and the {@code ThreadLocal}
 * instance is accessible; after a thread goes away, all of its copies of
 * thread-local instances are subject to garbage collection (unless other
 * references to these copies exist).
 *
 * @author  Josh Bloch and Doug Lea
 * @since   1.2
 * ThreadLocal 提供了线程本地的实例。它与普通变量的区别在于，
 * 每个使用该变量的线程都会初始化一个完全独立的实例副本。
 * ThreadLocal 变量通常被private static修饰。
 * 当一个线程结束时，它所使用的所有 ThreadLocal 相对的实例副本都可被回收。
 */
public class ThreadLocal<T> {
    /**
     * ThreadLocals rely on per-thread linear-probe hash maps attached
     * to each thread (Thread.threadLocals and
     * inheritableThreadLocals).  The ThreadLocal objects act as keys,
     * searched via threadLocalHashCode.  This is a custom hash code
     * (useful only within ThreadLocalMaps) that eliminates collisions
     * in the common case where consecutively constructed ThreadLocals
     * are used by the same threads, while remaining well-behaved in
     * less common cases.
     */
    /**
     * threadLocalHashCode来标识每一个ThreadLocal的唯一性
     * 通过 CAS 操作进行更新，每次 hash 操作的增量为 0x61c88647（不知为何）
     */
    private final int threadLocalHashCode = nextHashCode();

    /**
     * The next hash code to be given out. Updated atomically. Starts at
     * zero.
     */
    private static AtomicInteger nextHashCode =
        new AtomicInteger();

    /**
     * The difference between successively generated hash codes - turns
     * implicit sequential thread-local IDs into near-optimally spread
     * multiplicative hash values for power-of-two-sized tables.
     */
    private static final int HASH_INCREMENT = 0x61c88647;

    /**
     * Returns the next hash code.
     */
    private static int nextHashCode() {
        return nextHashCode.getAndAdd(HASH_INCREMENT);
    }

    /**
     * Returns the current thread's "initial value" for this
     * thread-local variable.  This method will be invoked the first
     * time a thread accesses the variable with the {@link #get}
     * method, unless the thread previously invoked the {@link #set}
     * method, in which case the {@code initialValue} method will not
     * be invoked for the thread.  Normally, this method is invoked at
     * most once per thread, but it may be invoked again in case of
     * subsequent invocations of {@link #remove} followed by {@link #get}.
     *
     * <p>This implementation simply returns {@code null}; if the
     * programmer desires thread-local variables to have an initial
     * value other than {@code null}, {@code ThreadLocal} must be
     * subclassed, and this method overridden.  Typically, an
     * anonymous inner class will be used.
     *
     * @return the initial value for this thread-local
     */
    protected T initialValue() {
        return null;
    }

    /**
     * Creates a thread local variable. The initial value of the variable is
     * determined by invoking the {@code get} method on the {@code Supplier}.
     *
     * @param <S> the type of the thread local's value
     * @param supplier the supplier to be used to determine the initial value
     * @return a new thread local variable
     * @throws NullPointerException if the specified supplier is null
     * @since 1.8
     */
    public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
        return new SuppliedThreadLocal<>(supplier);
    }

    /**
     * Creates a thread local variable.
     * @see #withInitial(java.util.function.Supplier)
     */
    public ThreadLocal() {
    }

    /**
     * Returns the value in the current thread's copy of this
     * thread-local variable.  If the variable has no value for the
     * current thread, it is first initialized to the value returned
     * by an invocation of the {@link #initialValue} method.
     *
     * @return the current thread's value of this thread-local
     */
    public T get() {
        //获取当前线程
        Thread t = Thread.currentThread();
        //获取当前线程的threadLocals
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            // 从当前线程的ThreadLocalMap获取相对应的Entry
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }
        //如果此时没有threadLocals则调用setInitialValue方法
        return setInitialValue();
    }

    /**
     * Variant of set() to establish initialValue. Used instead
     * of set() in case user has overridden the set() method.
     *
     * @return the initial value
     */
    private T setInitialValue() {
        //默认返回null，protected修饰的方法，可重写此方法，实现默认值自定义
        T value = initialValue();
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
        return value;
    }

    /**
     * Sets the current thread's copy of this thread-local variable
     * to the specified value.  Most subclasses will have no need to
     * override this method, relying solely on the {@link #initialValue}
     * method to set the values of thread-locals.
     *
     * @param value the value to be stored in the current thread's copy of
     *        this thread-local.
     */
    public void set(T value) {
        //获取当前线程
        Thread t = Thread.currentThread();
        //获取当前线程的ThreadLocalMap
        ThreadLocalMap map = getMap(t);
        //如果ThreadLocalMap不为空那么直接设值
        if (map != null)
            map.set(this, value);
        else
            //第一次调用set时map为空，
            // 那么new一个ThreadLocalMap实例并赋给Thread.threadLocals
            createMap(t, value);
    }

    /**
     * Removes the current thread's value for this thread-local
     * variable.  If this thread-local variable is subsequently
     * {@linkplain #get read} by the current thread, its value will be
     * reinitialized by invoking its {@link #initialValue} method,
     * unless its value is {@linkplain #set set} by the current thread
     * in the interim.  This may result in multiple invocations of the
     * {@code initialValue} method in the current thread.
     *
     * @since 1.5
     */
     public void remove() {
         //获取当前线程的ThreadLocalMap
         ThreadLocalMap m = getMap(Thread.currentThread());
         if (m != null)
             //如果不空那么移除当前的ThreadLocal
             m.remove(this);
     }

    /**
     * Get the map associated with a ThreadLocal. Overridden in
     * InheritableThreadLocal.
     *
     * @param  t the current thread
     * @return the map
     */
    /**
     * 返回Thread实例的成员变量threadLocals。
     * 它的定义在Thread内部，访问级别为package级别：
     */
    /**
     * 我们可以看出，每个Thread里面都有一个ThreadLocal.ThreadLocalMap成员变量，
     * 也就是说每个线程通过ThreadLocal.ThreadLocalMap与ThreadLocal相绑定，
     * 这样可以确保每个线程访问到的thread-local variable都是本线程的
     */
    ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;
    }

    /**
     * Create the map associated with a ThreadLocal. Overridden in
     * InheritableThreadLocal.
     *
     * @param t the current thread
     * @param firstValue value for the initial entry of the map
     */
    void createMap(Thread t, T firstValue) {
        t.threadLocals = new ThreadLocalMap(this, firstValue);
    }

    /**
     * Factory method to create map of inherited thread locals.
     * Designed to be called only from Thread constructor.
     *
     * @param  parentMap the map associated with parent thread
     * @return a map containing the parent's inheritable bindings
     */
    static ThreadLocalMap createInheritedMap(ThreadLocalMap parentMap) {
        return new ThreadLocalMap(parentMap);
    }

    /**
     * Method childValue is visibly defined in subclass
     * InheritableThreadLocal, but is internally defined here for the
     * sake of providing createInheritedMap factory method without
     * needing to subclass the map class in InheritableThreadLocal.
     * This technique is preferable to the alternative of embedding
     * instanceof tests in methods.
     */
    T childValue(T parentValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * An extension of ThreadLocal that obtains its initial value from
     * the specified {@code Supplier}.
     */
    static final class SuppliedThreadLocal<T> extends ThreadLocal<T> {

        private final Supplier<? extends T> supplier;

        //ThreadLocal的一个子类，重写了父类的initialValue方法
        SuppliedThreadLocal(Supplier<? extends T> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        @Override
        protected T initialValue() {
            return supplier.get();
        }
    }

    /**
     * ThreadLocalMap is a customized hash map suitable only for
     * maintaining thread local values. No operations are exported
     * outside of the ThreadLocal class. The class is package private to
     * allow declaration of fields in class Thread.  To help deal with
     * very large and long-lived usages, the hash table entries use
     * WeakReferences for keys. However, since reference queues are not
     * used, stale entries are guaranteed to be removed only when
     * the table starts running out of space.
     */
    static class ThreadLocalMap {

        /**
         * The entries in this hash map extend WeakReference, using
         * its main ref field as the key (which is always a
         * ThreadLocal object).  Note that null keys (i.e. entry.get()
         * == null) mean that the key is no longer referenced, so the
         * entry can be expunged from table.  Such entries are referred to
         * as "stale entries" in the code that follows.
         */
        /**
         * ThreadLocalMap 的每个 Entry 都是一个对 键 的弱引用，这一点从super(k)可看出。
         * 另外，每个 Entry 都包含了一个对 值 的强引用。
         *
         *
         * 使用弱引用的原因在于，当没有强引用指向 ThreadLocal 变量时，
         * 它可被回收，从而避免上文所述 ThreadLocal 不能被回收而造成的内存泄漏的问题。
         * 但是，这里又可能出现另外一种内存泄漏的问题。
         * ThreadLocalMap 维护 ThreadLocal 变量与具体实例的映射，
         * 当 ThreadLocal 变量被回收后，该映射的键变为 null，该 Entry 无法被移除。
         * 从而使得实例被该 Entry 引用而无法被回收造成内存泄漏。
         *
         * 注：Entry虽然是弱引用，但它是 ThreadLocal 类型的弱引用（也即上文所述它是对 键 的弱引用），
         * 而非具体实例的的弱引用，所以无法避免具体实例相关的内存泄漏。
         *
         *
         * 针对该问题，ThreadLocalMap 的 set 方法中，
         * 通过 replaceStaleEntry 方法将所有键为 null 的 Entry 的值设置为 null，从而使得该值可被回收。
         * 另外，会在 rehash或者remove方法中 方法中通过 expungeStaleEntry 方法将键和值为 null 的 Entry 设置为 null 从而使得该 Entry 可被回收。
         * 通过这种方式，ThreadLocal 可防止内存泄漏。
         *
         * 为了避免内存泄漏，我们可以在使用完ThreadLocal后，手动调用remove方法，以避免出现内存泄漏。
         *
         */
        static class Entry extends WeakReference<ThreadLocal<?>> {
            /** The value associated with this ThreadLocal. */
            Object value;

            Entry(ThreadLocal<?> k, Object v) {
                super(k);
                value = v;
            }
        }

        /**
         * The initial capacity -- MUST be a power of two.
         */
        private static final int INITIAL_CAPACITY = 16;

        /**
         * The table, resized as necessary.
         * table.length MUST always be a power of two.
         */
        private Entry[] table;

        /**
         * The number of entries in the table.
         * table中所使用的元素大小
         */
        private int size = 0;

        /**
         * The next size value at which to resize.
         */
        private int threshold; // Default to 0

        /**
         * Set the resize threshold to maintain at worst a 2/3 load factor.
         * 设置ThreadLocalMap中table的阈值，当table元素大于table大小三分之二时扩容
         */
        private void setThreshold(int len) {
            threshold = len * 2 / 3;
        }

        /**
         * Increment i modulo len.
         */
        private static int nextIndex(int i, int len) {
            return ((i + 1 < len) ? i + 1 : 0);
        }

        /**
         * Decrement i modulo len.
         */
        private static int prevIndex(int i, int len) {
            return ((i - 1 >= 0) ? i - 1 : len - 1);
        }

        /**
         * Construct a new map initially containing (firstKey, firstValue).
         * ThreadLocalMaps are constructed lazily, so we only create
         * one when we have at least one entry to put in it.
         */
        ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
            table = new Entry[INITIAL_CAPACITY];
            int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
            table[i] = new Entry(firstKey, firstValue);
            size = 1;
            setThreshold(INITIAL_CAPACITY);
        }

        /**
         * Construct a new map including all Inheritable ThreadLocals
         * from given parent map. Called only by createInheritedMap.
         *
         * @param parentMap the map associated with parent thread.
         */
        private ThreadLocalMap(ThreadLocalMap parentMap) {
            Entry[] parentTable = parentMap.table;
            int len = parentTable.length;
            setThreshold(len);
            table = new Entry[len];

            for (int j = 0; j < len; j++) {
                Entry e = parentTable[j];
                if (e != null) {
                    @SuppressWarnings("unchecked")
                    ThreadLocal<Object> key = (ThreadLocal<Object>) e.get();
                    if (key != null) {
                        Object value = key.childValue(e.value);
                        Entry c = new Entry(key, value);
                        int h = key.threadLocalHashCode & (len - 1);
                        while (table[h] != null)
                            h = nextIndex(h, len);
                        table[h] = c;
                        size++;
                    }
                }
            }
        }

        /**
         * Get the entry associated with key.  This method
         * itself handles only the fast path: a direct hit of existing
         * key. It otherwise relays to getEntryAfterMiss.  This is
         * designed to maximize performance for direct hits, in part
         * by making this method readily inlinable.
         *
         * @param  key the thread local object
         * @return the entry associated with key, or null if no such
         */
        /**
         * 由于采用了开放定址法，所以当前key的散列值和元素在数组的索引并不是完全对应的，
         * 首先取一个探测数（key的散列值），如果所对应的key就是我们所要找的元素，则返回，
         * 否则调用getEntryAfterMiss()
         */
        private Entry getEntry(ThreadLocal<?> key) {
            //获取index
            int i = key.threadLocalHashCode & (table.length - 1);
            Entry e = table[i];
            //如果获取到了，返回entry
            if (e != null && e.get() == key)
                return e;
            else
                //否则遍历table
                return getEntryAfterMiss(key, i, e);
        }

        /**
         * Version of getEntry method for use when key is not found in
         * its direct hash slot.
         *
         * @param  key the thread local object
         * @param  i the table index for key's hash code
         * @param  e the entry at table[i]
         * @return the entry associated with key, or null if no such
         */
        private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {
            Entry[] tab = table;
            int len = tab.length;

            while (e != null) {
                ThreadLocal<?> k = e.get();
                //找到对应的entry，直接返回
                if (k == key)
                    return e;
                if (k == null)
                    //该方法用于处理key == null，有利于GC回收，能够有效地避免内存泄漏。
                    expungeStaleEntry(i);
                else
                    //找下一个index处的Entry
                    i = nextIndex(i, len);
                e = tab[i];
            }
            //没找到返回null
            return null;
        }

        /**
         * Set the value associated with key.
         *
         * @param key the thread local object
         * @param value the value to be set
         */
        private void set(ThreadLocal<?> key, Object value) {

            // We don't use a fast path as with get() because it is at
            // least as common to use set() to create new entries as
            // it is to replace existing ones, in which case, a fast
            // path would fail more often than not.

            Entry[] tab = table;
            int len = tab.length;
            //获取index，因为len为2的n次方，因此采用&代替模运算
            int i = key.threadLocalHashCode & (len-1);

            for (Entry e = tab[i];
                 e != null; //当获取到的entry为空时，结束遍历，说明没有发生hash冲突，可以直接创建一个Entry，赋值到当前槽中
                 e = tab[i = nextIndex(i, len)]) { //说明entry不为空，且对应的key并未被回收，说明出现hash碰撞了，则获取下一个槽的Entry，

                // 当前index处已有entry
                ThreadLocal<?> k = e.get();

                if (k == key) {
                    // key（ThreadLocal）相同，更新value
                    e.value = value;
                    return;
                }

                // key == null，但是存在值（因为此处的e != null），说明之前的ThreadLocal对象已经被回收了
                // 因为key是在entry中时弱引用，当没有其他强引用引用key时，也就是主要entry中的弱引用时，key会被回收，
                // 那么需要清理之前Entry中的value，因为此value已经获取不到了，防止内存泄露。
                if (k == null) {
                    // 出现过期数据
                    // 遍历清洗过期数据并在index处插入新数据，并清理掉key为空的entry
                    replaceStaleEntry(key, value, i);
                    return;
                }
            }


            /*
             * ThreadLocal对应的key实例不存在也没有陈旧元素，new 一个
             * 当前key本应该插入的下标位置是在key.threadLocalHashCode & (len-1)位置的，
             * 但是由于该位置有冲突且对应的Entry的键不为null，因此一直往后找到一个为table中为null的位置
             * 这里再expungeStaleEntry方法的 if(h != i) 的判断条件就是因为这里
             */
            tab[i] = new Entry(key, value);
            int sz = ++size;
            // cleanSomeSlots 清楚陈旧的Entry（key == null）
            //因为key是在entry中时弱引用，当没有其他强引用引用key时，也就是主要entry中的弱引用时，key会被回收，
            // 那么需要清理之前Entry中的value，因为此value已经获取不到了，防止内存泄露。
            // 如果没有清理陈旧的 Entry 并且数组中的元素大于了阈值，则进行 rehash
            if (!cleanSomeSlots(i, sz) && sz >= threshold)
                rehash();
        }

        /**
         * Remove the entry for key.
         */
        private void remove(ThreadLocal<?> key) {
            Entry[] tab = table;
            int len = tab.length;
            //获取index
            int i = key.threadLocalHashCode & (len-1);
            //从i遍历table
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                //找到了
                if (e.get() == key) {
                    //调用clear方法，该方法会把key的引用清除
                    e.clear();
                    //清除key为null的entry
                    expungeStaleEntry(i);
                    return;
                }
            }
        }

        /**
         * Replace a stale entry encountered during a set operation
         * with an entry for the specified key.  The value passed in
         * the value parameter is stored in the entry, whether or not
         * an entry already exists for the specified key.
         *
         * As a side effect, this method expunges all stale entries in the
         * "run" containing the stale entry.  (A run is a sequence of entries
         * between two null slots.)
         *
         * @param  key the key
         * @param  value the value to be associated with key
         * @param  staleSlot index of the first stale entry encountered while
         *         searching for key.
         *
         * 当插入数据时，并且入参key对应的目标槽(key.threadLocalHashCode & (len-1))不为空，即出现hash冲突时，且目标槽中的entry是无效的，即entry的key==nul
         * 将来到这儿，
         *
         *
         * 从当前槽staleSlot处，尝试找槽中tab中的key = 入参key的槽
         * 1. 如果找到了，那么直接替换槽中的value即可，并且尝试清除一些 槽不空但是对应的key为空的槽，help gc,防止内存泄露 @1
         * 2. 如果没找到那么说明此key第一次来，直接在staleSlot处，插入当前 key value即可，同样的尝试清除一些 槽不空但是对应的key为空的槽
         */
        private void replaceStaleEntry(ThreadLocal<?> key, Object value,
                                       int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;
            Entry e;

            // Back up to check for prior stale entry in current run.
            // We clean out whole runs at a time to avoid continual
            // incremental rehashing due to garbage collector freeing
            // up refs in bunches (i.e., whenever the collector runs).
            //擦除槽的index的开始位置
            int slotToExpunge = staleSlot;
            //获取当前staleSlot的前一个不为空Entry中键为空的index
            for (int i = prevIndex(staleSlot, len); (e = tab[i]) != null; i = prevIndex(i, len)) {
                if (e.get() == null)
                    slotToExpunge = i;
            }

            // Find either the key or trailing null slot of run, whichever
            // occurs first
            //从当前staleSlot的下一个index开始遍历table
            for (int i = nextIndex(staleSlot, len); (e = tab[i]) != null; i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();

                // If we find key, then we need to swap it
                // with the stale entry to maintain hash table order.
                // The newly stale slot, or any other stale slot
                // encountered above it, can then be sent to expungeStaleEntry
                // to remove or rehash all of the other entries in run.
                /*
                 * 如果在table中找到了键为当前ThreadLocal的Entry，那么替换其value
                 * 并且staleSlot与当前index的Entry交换，
                 * 注：个人觉得交换的原因应该是为了下一次ThreadLocal.get()能直接命中
                 * @1
                 */
                if (k == key) {
                    e.value = value;

                    tab[i] = tab[staleSlot];
                    tab[staleSlot] = e;

                    // Start expunge at preceding stale entry if it exists
                    /*
                     * 如果slotToExpunge == staleSlot那么说明往前没找到Entry不为空且键为null的Entry(在table的下标不为空之前找到)
                     * 那么上面中index为i与staleSlot的Entry已经交换了，说明只有index为i的staleSlot
                     * 那么将i赋值给slotToExpunge
                     */
                    if (slotToExpunge == staleSlot)
                        slotToExpunge = i;
                    /*
                     * expungeStaleEntry方法是清除index为expungeStaleEntry的entry，并且会判断与之连续的一整块entry是否需要清除
                     * 那么如果table的长度很长，有很多entry不为空的连续的碎片空间(table大小为16，其中 123 567 789)，
                     * 那么expungeStaleEntry只会判断其中一块连续的，碰到下一个为空的就停止判断，且返回值就是当前空的index的值
                     * 因此需要将返回值作为cleanSomeSlots的参数，并且cleanSomeSlots中又会调用expungeStaleEntry就这样递归下去
                     * 直至清楚大部分为空的数据
                     *
                     */
                    cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
                    return;
                }

                // If we didn't find stale entry on backward scan, the
                // first stale entry seen while scanning for key is the
                // first still present in the run.
                //如果当前index的Entry不为空且键为null且slotToExpunge == staleSlotslotTo
                if (k == null && slotToExpunge == staleSlot)
                    slotToExpunge = i;
            }

            // If key not found, put new entry in stale slot
            //如果没有找到，那么擦除当前index的Entry的value，help GC
            //且new一个Entry在当前下标处
            //@2
            tab[staleSlot].value = null;
            tab[staleSlot] = new Entry(key, value);

            // If there are any other stale entries in run, expunge them
            //不相等说明存在其它需要擦除的Entry
            if (slotToExpunge != staleSlot)
                cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
        }

        /**
         * Expunge a stale entry by rehashing any possibly colliding entries
         * lying between staleSlot and the next null slot.  This also expunges
         * any other stale entries encountered before the trailing null.  See
         * Knuth, Section 6.4
         *
         * @param staleSlot index of slot known to have null key
         * @return the index of the next null slot after staleSlot
         * (all between staleSlot and this slot will have been checked
         * for expunging).
         */
        /**
         *
         * 从当前staleSlot槽开始，尝试擦除一段连续槽，
         * 1.其中对应的槽不空，但是槽中tab的key为空的，需要被擦除，当遍历到第一个槽为空时结束循环 @1
         * 2.当槽中tab不为空，且tab的key也不为空时，尝试将此key移动到原本的槽中(k.threadLocalHashCode & (len - 1)的槽处)，
         *   如果原本槽中有值，那么一直向后找知道找到一个为空的槽，应该是为了下一次ThreadLocal.get()能快速命中 @2
         *
         *
         */
        private int expungeStaleEntry(int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;

            // expunge entry at staleSlot
            // 擦除当前index处的Entry的value，
            tab[staleSlot].value = null;
            // 擦除当前table中index处的值
            tab[staleSlot] = null;
            size--;

            // Rehash until we encounter null
            Entry e;
            int i;
            // 从当前staleSlot的下一个Entry开始遍历
            for (i = nextIndex(staleSlot, len); (e = tab[i]) != null; i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();
                //如果Entry的键为null,那么同样擦除当前index处的Entry的value，擦除当前table中index处的值，
                // help GC
                //@1
                if (k == null) {
                    e.value = null;
                    tab[i] = null;
                    size--;
                } else {
                    // @2
                    // 计算新index
                    int h = k.threadLocalHashCode & (len - 1);
                    //若计算出来的index不为当前的index，说明该k在set时发生了冲突，被放在index为h之后的空的的index中，也就是当前位置
                    if (h != i) {
                        //那么将当前的index置空
                        tab[i] = null;

                        // Unlike Knuth 6.4 Algorithm R, we must scan until
                        // null because multiple entries could have been stale.
                        // 一直扫到最后一个非空位置，将其值置为碰撞处第一个entry。
                        //尝试着将k放到原本h的位置，如果不行那么往后找一个合适的位置，就是为了里原本的h的位置近一点
                        //还是为了下一次ThreadLocal.get()能快速命中
                        while (tab[h] != null)
                            h = nextIndex(h, len);
                        tab[h] = e;
                    }
                }
            }
            return i;
        }

        /**
         * Heuristically scan some cells looking for stale entries.
         * This is invoked when either a new element is added, or
         * another stale one has been expunged. It performs a
         * logarithmic number of scans, as a balance between no
         * scanning (fast but retains garbage) and a number of scans
         * proportional to number of elements, that would find all
         * garbage but would cause some insertions to take O(n) time.
         *
         * @param i a position known NOT to hold a stale entry. The
         * scan starts at the element after i.
         *
         * @param n scan control: {@code log2(n)} cells are scanned,
         * unless a stale entry is found, in which case
         * {@code log2(table.length)-1} additional cells are scanned.
         * When called from insertions, this parameter is the number
         * of elements, but when from replaceStaleEntry, it is the
         * table length. (Note: all this could be changed to be either
         * more or less aggressive by weighting n instead of just
         * using straight log n. But this version is simple, fast, and
         * seems to work well.)
         *
         * @return true if any stale entries have been removed.
         *
         * 从i的下一个槽开始，如果槽中的entry的key==null，那么调用expungeStaleEntry清除无用的槽
         * 当槽中数据都是有效数据时，n决定循环次数
         */
        private boolean cleanSomeSlots(int i, int n) {
            boolean removed = false;
            Entry[] tab = table;
            int len = tab.length;
            //至于为什么使用table长度来决定遍历次数，官方给出的解释是这个方法简单、快速，并且效果不错。
            do {
                i = nextIndex(i, len);
                Entry e = tab[i];
                if (e != null && e.get() == null) {
                    //如果当前entry不为null且对应的键为null，那么需要清除
                    //重置n的值
                    n = len;
                    //有数据清除removed为true
                    removed = true;
                    //清除下标为i的entry并且哦按段与之连续的entry是否需要清除，
                    i = expungeStaleEntry(i);
                }

            } while ( (n >>>= 1) != 0);//无符号右移动1位
            return removed;
        }

        /**
         * Re-pack and/or re-size the table. First scan the entire
         * table removing stale entries. If this doesn't sufficiently
         * shrink the size of the table, double the table size.
         */
        private void rehash() {
            //清理掉所有key为null的entry
            expungeStaleEntries();

            // Use lower threshold for doubling to avoid hysteresis
            //如果清理后size超过阈值的3/4，则进行扩容

            if (size >= threshold - threshold / 4)
                resize();
        }

        /**
         * Double the capacity of the table.
         */
        private void resize() {
            Entry[] oldTab = table;
            int oldLen = oldTab.length;
            //新的table大小为之前的2倍
            int newLen = oldLen * 2;
            Entry[] newTab = new Entry[newLen];
            int count = 0;

            //遍历oldTab
            for (int j = 0; j < oldLen; ++j) {
                Entry e = oldTab[j];
                if (e != null) {
                    ThreadLocal<?> k = e.get();
                    //如果entry的键为null，则清空value
                    if (k == null) {
                        e.value = null; // Help the GC
                    } else {
                        //计算新表的index
                        int h = k.threadLocalHashCode & (newLen - 1);
                        //如果发生了冲突，则往后找，直至没有冲突
                        while (newTab[h] != null)
                            h = nextIndex(h, newLen);
                        newTab[h] = e;
                        count++;
                    }
                }
            }

            //设置table的阈值为table大小的2/3
            setThreshold(newLen);
            size = count;
            table = newTab;
        }

        /**
         * Expunge all stale entries in the table.
         */
        private void expungeStaleEntries() {
            Entry[] tab = table;
            int len = tab.length;
            for (int j = 0; j < len; j++) {
                Entry e = tab[j];
                if (e != null && e.get() == null)
                    expungeStaleEntry(j);
            }
        }
    }
}
