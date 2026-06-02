package com.spacemate.common.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

public final class BeanMergeUtils {

    private BeanMergeUtils() {
    }

    public static String[] nullPropertyNames(Object source) {
        BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(source.getClass(), Object.class);
        } catch (IntrospectionException e) {
            return new String[0];
        }
        List<String> nullNames = new ArrayList<>();
        for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
            try {
                Object value = pd.getReadMethod().invoke(source);
                if (value == null) {
                    nullNames.add(pd.getName());
                }
            } catch (Exception ignore) {
            }
        }
        return nullNames.toArray(new String[0]);
    }
}


