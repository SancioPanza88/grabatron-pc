import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Verifica che ogni riferimento Field/Method del bytecode del gioco si risolva. */
public class LinkCheck {
    static String desc(Class<?> c) {
        if (c.isPrimitive()) {
            if (c == void.class) return "V"; if (c == int.class) return "I"; if (c == boolean.class) return "Z";
            if (c == byte.class) return "B"; if (c == char.class) return "C"; if (c == short.class) return "S";
            if (c == long.class) return "J"; if (c == float.class) return "F"; return "D";
        }
        if (c.isArray()) return "[" + desc(c.getComponentType());
        return "L" + c.getName().replace('.', '/') + ";";
    }
    static boolean findField(Class<?> c, String n, String d) {
        if (c == null) return false;
        for (Field f : c.getDeclaredFields()) if (f.getName().equals(n) && desc(f.getType()).equals(d)) return true;
        for (Class<?> i : c.getInterfaces()) if (findField(i, n, d)) return true;
        return findField(c.getSuperclass(), n, d);
    }
    static boolean findMethod(Class<?> c, String n, String d) {
        if (c == null) return false;
        if (n.equals("\"<init>\"") || n.equals("<init>")) {
            for (Constructor<?> k : c.getDeclaredConstructors()) {
                StringBuilder s = new StringBuilder("(");
                for (Class<?> p : k.getParameterTypes()) s.append(desc(p));
                if ((s + ")V").equals(d)) return true;
            }
            return false;
        }
        for (Method m : c.getDeclaredMethods()) {
            StringBuilder s = new StringBuilder("(");
            for (Class<?> p : m.getParameterTypes()) s.append(desc(p));
            s.append(")").append(desc(m.getReturnType()));
            if (m.getName().equals(n) && s.toString().equals(d)) return true;
        }
        for (Class<?> i : c.getInterfaces()) if (findMethod(i, n, d)) return true;
        return findMethod(c.getSuperclass(), n, d);
    }
    public static void main(String[] a) throws Exception {
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        String line; int bad = 0, n = 0;
        while ((line = r.readLine()) != null) {
            String[] p = line.split(" ");  // kind owner name desc
            if (p.length < 4 || p[1].startsWith("[")) continue;
            n++;
            try {
                Class<?> c = Class.forName(p[1].replace('/', '.'), false, LinkCheck.class.getClassLoader());
                boolean ok = p[0].equals("Field") ? findField(c, p[2], p[3]) : findMethod(c, p[2], p[3]);
                if (!ok) { bad++; System.out.println("MANCA " + line); }
            } catch (Throwable t) { bad++; System.out.println("CLASSE " + line + " -> " + t); }
        }
        System.out.println("controllati " + n + ", problemi " + bad);
    }
}
