package runtime;

/*
 * @(#)HashMap.java	1.51 02/01/24
 *
 * Copyright 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

import  java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

final class NameCodeMap
{
    /**
     * The default initial capacity - MUST be a power of two.
     */
    static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load fast used when none specified in constructor.
     **/
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * The table, resized as necessary. Length MUST Always be a power of two.
     */
    transient Entry[] table;

    /**
     * The number of key-value mappings contained in this identity hash map.
     */
    transient int size;
  
    /**
     * The next size value at which to resize (capacity * load factor).
     * @serial
     */
    int threshold;
  
    /**
     * The load factor for the hash table.
     *
     * @serial
     */
    final float loadFactor;

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and load factor.
     *
     * @param  initialCapacity The initial capacity.
     * @param  loadFactor      The load factor.
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive.
     */
    public NameCodeMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                                               initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                                               loadFactor);

        // Find a power of 2 >= initialCapacity
        int capacity = 1;
        while (capacity < initialCapacity) 
            capacity <<= 1;
    
        this.loadFactor = loadFactor;
        threshold = (int)(capacity * loadFactor);
        table = new Entry[capacity];
    }
  
    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param  initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    public NameCodeMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public NameCodeMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        threshold = (int)(DEFAULT_INITIAL_CAPACITY);
        table = new Entry[DEFAULT_INITIAL_CAPACITY];
    }

    
    public Entry get(String uri, String localName) {
        
        int hash = uri.hashCode()^localName.hashCode();
        hash = hash - (hash << 7);  // i.e., -127 * h
        int h = hash;
        
        int i = h & (table.length-1);
        Entry e = table[i]; 
        while (true) {
            if (e == null)
                return e;   // not found
            if (e.hash == hash && uri.equals(e.uri) && localName.equals(e.localName)) 
                return e;   // found
            e = e.next;
        }
    }

    public Object put(String uri, String localName, int nameCode) {
        
        int hash = uri.hashCode()^localName.hashCode();
        hash = hash - (hash << 7);  // i.e., -127 * h
        int h = hash;
        
        int i = h & (table.length-1);

        for (Entry e = table[i]; e != null; e = e.next) {
            if (e.hash == hash && uri.equals(e.uri) && localName.equals(e.localName)) 
                throw new InternalError("duplicate entry");
        }

        addEntry(hash, uri, localName, nameCode, i);
        return null;
    }


    /**
     * Rehashes the contents of this map into a new <tt>HashMap</tt> instance
     * with a larger capacity. This method is called automatically when the
     * number of keys in this map exceeds its capacity and load factor.
     *
     * @param newCapacity the new capacity, MUST be a power of two.
     */
    void resize(int newCapacity) {
        // assert (newCapacity & -newCapacity) == newCapacity; // power of 2
        Entry[] oldTable = table;
        int oldCapacity = oldTable.length;
    
        // check if needed
        if (size < threshold || oldCapacity > newCapacity) 
            return;
    
        Entry[] newTable = new Entry[newCapacity];
        transfer(newTable);
        table = newTable;
        threshold = (int)(newCapacity * loadFactor);
    }

    /** 
     * Transfer all entries from current table to newTable.
     */
    void transfer(Entry[] newTable) {
        Entry[] src = table;
        int newCapacity = newTable.length;
        for (int j = 0; j < src.length; j++) {
            Entry e = src[j];
            if (e != null) {
                src[j] = null;
                do {
                    Entry next = e.next;
                    int length = newCapacity;
                    int i = e.hash & (length-1);  
                    e.next = newTable[i];
                    newTable[i] = e;
                    e = next;
                } while (e != null);
            }
        }
    }

    static final class Entry {
        final String uri;
        final String localName;
        
        final int nameCode;
        final HashMap startTagCache = new HashMap();

        // internal variables for this map implementation
        final int hash;
        Entry next;

        /**
         * Create new entry.
         */
        Entry(int h, String k1, String k2, int nameCode, Entry n) { 
            this.nameCode = nameCode;
            next = n;
            uri = k1;
            localName = k2;
            hash = h;
        }
    }

    /**
     * Add a new entry with the specified key, value and hash code to
     * the specified bucket.  It is the responsibility of this 
     * method to resize the table if appropriate.
     *
     * Subclass overrides this to alter the behavior of put method.
     */
    void addEntry(int hash, String uri, String localName, int nameCode, int bucketIndex) {
        table[bucketIndex] = new Entry(hash, uri, localName, nameCode, table[bucketIndex]);
        if (size++ >= threshold) 
            resize(2 * table.length);
    }
}
