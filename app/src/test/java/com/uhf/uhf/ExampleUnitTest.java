package com.uhf.uhf;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    static AtomicInteger atomicInteger = null;
    @BeforeClass
    public static void init() {
        atomicInteger = new AtomicInteger(0);
    }
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);

    }

    @Test
    public void testIncreatment() {
        assertEquals(1,atomicInteger.incrementAndGet());
    }
}