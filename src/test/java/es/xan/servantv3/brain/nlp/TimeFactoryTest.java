package es.xan.servantv3.brain.nlp;

import org.junit.Test;

import static org.junit.Assert.*;

public class TimeFactoryTest {

    @Test
    public void test_1() {
        long value = TimeFactory.findTimeAndTransform("apagar caldera a las doce y cinco");
        System.out.println(value);
    }
}