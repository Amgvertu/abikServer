package info.prorabka.abik.server.common.file;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/v1/files")
@Tag(name = "Files", description = "Загрузка и выдача файлов (аватары, логотипы)")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    private final FileStorageService storage;

    public FileController(FileStorageService storage) {
        this.storage = storage;
    }

    @Operation(summary = "Загрузить файл")
    @ApiResponse(responseCode = "200", description = "URL файла")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(
            @Parameter(description = "Файл (jpg/png/webp, до 5 МБ)")
            @RequestParam("file") MultipartFile file) {
        String url = storage.store(file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @Operation(summary = "Получить файл по имени")
    @GetMapping("/{fileName}")
    public ResponseEntity<FileSystemResource> get(@PathVariable String fileName) {
        Path p = storage.load(fileName);
        String contentType = storage.detectContentType(fileName);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .body(new FileSystemResource(p));
    }
}