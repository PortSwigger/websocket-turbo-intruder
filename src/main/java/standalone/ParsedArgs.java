package standalone;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

record ParsedArgs(String scriptCode, String request, String endpoint, String message, String logFilePath, boolean debug) {
    public static ParsedArgs parse(String[] args) throws IOException {
        if (args.length < 1) throw new IllegalArgumentException("Missing script file.");

        String scriptCode = readFile(args[0]);

        String request = "";
        String message = "";
        String endpoint = "";
        String logFilePath = "";
        boolean debug = false;

        if (args.length > 1) {
            request = readFile(args[1]);
            if (!request.contains("\r\n")) request = request.replace("\n","\r\n");
            endpoint= args.length > 2 ? args[2] : "";
            message = args.length > 3 ? args[3] : "";
            logFilePath = args.length > 4 ? args[4] : "results.json";
            debug = args.length > 5 && Boolean.parseBoolean(args[5]);
        }

        return new ParsedArgs(scriptCode, request, endpoint, message, logFilePath, debug);
    }

    private static String readFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(new File(filePath).toPath()));
    }

    private static byte[] readRawFile(String filePath) throws IOException {
        return Files.readAllBytes(new File(filePath).toPath());
    }
}
