package com.codingcat.changelogs.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public final class ResourceUtil {
    private static final ClassLoader classLoader = ResourceUtil.class.getClassLoader();

    public static @NotNull Map<String, String> readResourcesAsString(@NotNull String dirPath) {
        try {
            Collection<String> resources = listResources(dirPath);
            Map<String, String> data = new HashMap<>();
            for (String resource : resources) {
                Path path = Path.of(dirPath).resolve(resource);
                InputStream stream = classLoader.getResourceAsStream(path.toString());
                if (stream == null) continue;
                String string = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                data.put(resource, string);
                stream.close();
            }
            return data;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read jar resources", e);
        }
    }

    public static @NotNull Collection<String> listResources(@NotNull String dirPath) {
        try {
            URI uri = Objects.requireNonNull(classLoader.getResource(dirPath), "Resource not found").toURI();
            FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
            Path path = fileSystem.getPath(dirPath);
            Collection<String> resources;
            try (Stream<Path> stream = Files.walk(path, 1)) {
                resources = stream.map(p -> p.getFileName().toString()).toList();
            }
            fileSystem.close();
            return resources;
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Failed to list jar resources at " + dirPath, e);
        }
    }
}
