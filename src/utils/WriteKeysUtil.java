package utils;

import java.io.FileWriter;
import java.io.IOException;

public class WriteKeysUtil {
    public static void writeKeysToFile(String type, String filePath, String keys) {
        try (FileWriter fw = new FileWriter(filePath)) {
            if (type.equals("RSA")) {
                String[] keysPart = keys.split("\\|");
                fw.write(keysPart[0] + "\n");
                fw.write(keysPart[1] + "\n");
            } else if (type.equals("Fernet")) {
                fw.write("Fernet=" + keys);
            }
        } catch (IOException ex) {
            System.out.println("Error writing to: " + filePath);
            ex.printStackTrace();
        }
    }
}
