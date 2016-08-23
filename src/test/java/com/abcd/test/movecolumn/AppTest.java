package com.abcd.test.movecolumn;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest
{
	@Test
    public void testSet(){
    	Set<Long> set=new HashSet<Long>();
    	set.add(2l);
    	System.out.println(set.contains(3l));
    }
}
