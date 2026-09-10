package dev.folderion.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.stream.Stream;

final class Io {

    private Io() {
    }

    static void writeText(Path path, String text) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, text == null ? "" : text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FolderionException("Failed to write text: " + path, e);
        }
    }

    static void writeBytes(Path path, byte[] bytes) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
        } catch (IOException e) {
            throw new FolderionException("Failed to write bytes: " + path, e);
        }
    }

    static String readText(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FolderionException("Failed to read text: " + path, e);
        }
    }

    static void deleteRecursively(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new FolderionException("Failed to delete: " + path, e);
                }
            });
        } catch (IOException e) {
            throw new FolderionException("Failed to walk: " + root, e);
        }
    }

    static void moveReplace(Path source, Path target) {
        try {
            Files.createDirectories(target.getParent());
            if (Files.exists(target)) {
                deleteRecursively(target);
            }
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException atomicFailed) {
            try {
                if (Files.exists(target)) {
                    deleteRecursively(target);
                }
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new FolderionException("Failed to move " + source + " -> " + target, e);
            }
        }
    }

    static void copyRecursively(Path source, Path target) {
        try {
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path relative = source.relativize(dir);
                    Path dest = relative.toString().isEmpty() ? target : target.resolve(relative);
                    Files.createDirectories(dest);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path dest = target.resolve(source.relativize(file));
                    Files.createDirectories(dest.getParent());
                    Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new FolderionException("Failed to copy " + source + " -> " + target, e);
        }
    }
}
