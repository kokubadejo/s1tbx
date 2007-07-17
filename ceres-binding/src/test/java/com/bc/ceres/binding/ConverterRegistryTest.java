package com.bc.ceres.binding;

import junit.framework.TestCase;

import java.io.File;
import java.util.Date;
import java.util.regex.Pattern;

public class ConverterRegistryTest extends TestCase {

    public void testPrimitiveTypes()  {
        final ConverterRegistry r = ConverterRegistry.getInstance();
        assertNotNull(r.getConverter(Boolean.TYPE));
        assertNotNull(r.getConverter(Character.TYPE));
        assertNotNull(r.getConverter(Byte.TYPE));
        assertNotNull(r.getConverter(Short.TYPE));
        assertNotNull(r.getConverter(Integer.TYPE));
        assertNotNull(r.getConverter(Long.TYPE));
        assertNotNull(r.getConverter(Float.TYPE));
        assertNotNull(r.getConverter(Double.TYPE));
    }

    public void testPrimitiveTypeWrappers()  {
        final ConverterRegistry r = ConverterRegistry.getInstance();
        assertNotNull(r.getConverter(Boolean.class));
        assertNotNull(r.getConverter(Character.class));
        assertNotNull(r.getConverter(Byte.class));
        assertNotNull(r.getConverter(Short.class));
        assertNotNull(r.getConverter(Integer.class));
        assertNotNull(r.getConverter(Long.class));
        assertNotNull(r.getConverter(Float.class));
        assertNotNull(r.getConverter(Double.class));
    }

    public void testObjects()  {
        final ConverterRegistry r = ConverterRegistry.getInstance();
        assertNotNull(r.getConverter(String.class));
        assertNotNull(r.getConverter(File.class));
        assertNotNull(r.getConverter(Interval.class));
        assertNotNull(r.getConverter(Date.class));
        assertNotNull(r.getConverter(Pattern.class));
        // todo - assertNotNull(r.getConverter(Color.class));
    }

    public void testDerivedObjects()  {
        final ConverterRegistry r = ConverterRegistry.getInstance();

        assertSame(r.getConverter(File.class), r.getConverter(MyFile.class));
        assertSame(r.getConverter(Date.class), r.getConverter(java.sql.Date.class));
    }

    private static class MyFile extends File {
        public MyFile() {
            super("");
        }
    }
}
