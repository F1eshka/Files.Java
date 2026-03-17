import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.*;

public class Main {
    public static void main(String[] args) throws IOException {
        String rootPath;
        if (args.length > 0) {
            rootPath = args[0];
        } else {
            rootPath = System.getProperty("os.name").toLowerCase().contains("win") ? "C:\\" : "/";
        }

        Path root = Paths.get(rootPath);
        if (!Files.exists(root)) {
            System.err.println("Шлях не існує: " + rootPath);
            System.exit(1);
        }

        Map<String, Long> map = new ConcurrentHashMap<>();
        AtomicLong visited = new AtomicLong(0);

        System.out.println("Сканування: " + root.toAbsolutePath());
        System.out.println("Зачекайте...\n");

        long startTime = System.currentTimeMillis();

        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path f, BasicFileAttributes a) {
                String name = f.getFileName().toString();
                int i = name.lastIndexOf('.');
                String ext = (i > 0 && i < name.length() - 1)
                        ? name.substring(i + 1).toLowerCase()
                        : "(none)";
                map.merge(ext, 1L, Long::sum);

                long count = visited.incrementAndGet();
                if (count % 10_000 == 0) {
                    System.out.printf("\r  Оброблено файлів: %,d ...", count);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path f, IOException e) {
                return FileVisitResult.CONTINUE;
            }
        });

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.printf("%n%n  Готово за %d.%03d сек%n%n", elapsed / 1000, elapsed % 1000);

        List<Map.Entry<String, Long>> top = map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(50)
                .toList();

        long total = map.values().stream().mapToLong(Long::longValue).sum();

        String sep = "+-----+----------------------+------------+";
        String sepShort = "+-----+----------------------+------------+";

        System.out.println(sepShort);
        System.out.printf("| %-3s | %-20s | %10s |%n", "№", "Розширення", "Кількість");
        System.out.println(sep);

        for (int i = 0; i < top.size(); i++) {
            System.out.printf("| %-3d | %-20s | %,10d |%n",
                    i + 1,
                    top.get(i).getKey(),
                    top.get(i).getValue());
        }

        System.out.println(sep);
        System.out.printf("| %-3s | %-20s | %,10d |%n", "", "TOTAL:", total);
        System.out.println(sep);
        System.out.printf("  Унікальних розширень: %,d%n", map.size());
    }
}
