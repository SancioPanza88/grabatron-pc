package desktop;

import java.io.*;
import java.util.HashMap;

/** Tabella delle risorse "raw" del gioco: nome -> file, id Android -> nome. */
public final class Res {
    private static final HashMap<String, File> byName = new HashMap<>();
    private static final HashMap<Integer, String> nameById = new HashMap<>();
    private static final HashMap<String, Integer> idByName = new HashMap<>();
    public static File dataDir;

    public static void init(File dir) throws IOException {
        dataDir = dir;
        File raw = new File(dir, "raw");
        File[] files = raw.listFiles();
        if (files == null) throw new FileNotFoundException("Cartella dati mancante: " + raw.getAbsolutePath());
        for (File f : files) {
            String n = f.getName();
            int dot = n.indexOf('.');
            byName.put(dot < 0 ? n : n.substring(0, dot), f);
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(new File(dir, "raw_ids.txt")), "UTF-8"))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.trim().split("\\s+");
                if (p.length != 2) continue;
                int id = (int) Long.parseLong(p[1].replace("0x", ""), 16);
                nameById.put(id, p[0]);
                idByName.put(p[0], id);
            }
        }
    }

    private static String clean(String basename) {
        if (basename.startsWith("/")) basename = basename.substring(1);
        return basename;
    }

    /** Come getIdentifier(":raw" + basename): 0 se non esiste. */
    public static int id(String basename) {
        Integer i = idByName.get(clean(basename));
        return (i == null || !byName.containsKey(clean(basename))) ? 0 : i;
    }

    public static File file(String basename) { return byName.get(clean(basename)); }

    public static File fileById(int id) {
        String n = nameById.get(id);
        return n == null ? null : byName.get(n);
    }

    public static byte[] bytesById(int id) throws IOException {
        File f = fileById(id);
        if (f == null) throw new FileNotFoundException("risorsa 0x" + Integer.toHexString(id));
        try (FileInputStream in = new FileInputStream(f)) { return in.readAllBytes(); }
    }
}
