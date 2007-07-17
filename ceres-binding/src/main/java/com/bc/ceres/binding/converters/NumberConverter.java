package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConversionException;

public abstract class NumberConverter implements Converter {

    public Class<?> getValueType() {
        return Number.class;
    }

    public Object parse(String value) throws ConversionException {
        if (value.isEmpty()) {
            return null;
        }
        try {
            return parseNumber(com.bc.ceres.binding.converters.NumberConverter.trimNumberString(value));
        } catch (Exception e) {
            throw new ConversionException(e);
        }
    }

    protected abstract Object parseNumber(String value) throws NumberFormatException;

    public String format(Object value) throws ConversionException {
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    private static String trimNumberString(String s) {
        s = s.trim();
        if (s.startsWith("+")) {
            s = s.substring(1);
        }
        return s;
    }
}
