package pdf.util;

import org.apache.commons.beanutils.BeanComparator;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * PDF-SORT
 * @author xiaozj
 */
public class SortUtil {

    public SortUtil() {
    }

    public static <V> void sort(List<V> list, final String... properties) {
        Collections.sort(list, new Comparator<V>() {
            @Override
            public int compare(V o1, V o2) {
                if (o1 == null && o2 == null) {
                    return 0;
                } else if (o1 == null) {
                    return -1;
                } else if (o2 == null) {
                    return 1;
                } else {
                    int i = properties.length;

                    for(byte b = 0; b < i; ++b) {
                        String property = properties[b];
                        BeanComparator beanComparator = new BeanComparator(property);
                        int result = beanComparator.compare(o1, o2);
                        if (result != 0) {
                            return result;
                        }
                    }

                    return 0;
                }
            }
        });
    }
}

