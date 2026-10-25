package info.prorabka.abik.server.common.file;

import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.common.exception.NotFoundException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Локальное хранилище файлов (аватары, логотипы).
 * На первом этапе — просто папка на диске.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final Set<String> ALLOWED_MIME = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");

    @Value("${app.files.dir:./uploads}")
    private String filesDir;

    @Value("${app.files.max-size-bytes:5242880}")
    private long maxSizeBytes;

    private Path storagePath;

    @PostConstruct
    void init() throws IOException {
        storagePath = Paths.get(filesDir).toAbsolutePath().normalize();
        Files.createDirectories(storagePath);
        log.info("Файловое хранилище: {}", storagePath);
    }

    /** Сохранить файл, вернуть URL вида /api/v1/files/<uuid>.<ext>. */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Файл не передан");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new BadRequestException("Файл больше " + (maxSizeBytes / 1024 / 1024) + " МБ");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME.contains(contentType)) {
            throw new BadRequestException("Разрешены только jpeg, png, webp");
        }

        String originalName = file.getOriginalFilename();
        String ext = extractExt(originalName);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BadRequestException("Недопустимое расширение");
        }

        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = storagePath.resolve(fileName);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Ошибка сохранения файла {}", fileName, e);
            throw new RuntimeException("Не удалось сохранить файл", e);
        }

        return "/api/v1/files/" + fileName;
    }

    /** Прочитать файл по имени. */
    public Path load(String fileName) {
        // Защита от path traversal
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new BadRequestException("Некорректное имя файла");
        }
        Path p = storagePath.resolve(fileName).normalize();
        if (!p.startsWith(storagePath) || !Files.exists(p)) {
            throw new NotFoundException("Файл не найден");
        }
        return p;
    }

    public String detectContentType(String fileName) {
        String ext = extractExt(fileName);
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png"  -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    private String extractExt(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
    }
}