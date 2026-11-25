package info.prorabka.abik.server.common.file;

import info.prorabka.abik.server.common.exception.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/** Центрально кадрирует аватар и сохраняет его как JPEG 512 × 512. */
@Component
public class AvatarImageProcessor {

    static final int AVATAR_SIZE = 512;
    private static final long MAX_SOURCE_PIXELS = 40_000_000L;
    private static final float JPEG_QUALITY = 0.86f;

    public void writeNormalized(MultipartFile file, Path target) {
        BufferedImage source = readImage(file);
        BufferedImage normalized = new BufferedImage(AVATAR_SIZE, AVATAR_SIZE, BufferedImage.TYPE_INT_RGB);
        try {
            int side = Math.min(source.getWidth(), source.getHeight());
            int x = (source.getWidth() - side) / 2;
            int y = (source.getHeight() - side) / 2;
            BufferedImage crop = source.getSubimage(x, y, side, side);

            Graphics2D graphics = normalized.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.drawImage(crop, 0, 0, AVATAR_SIZE, AVATAR_SIZE, null);
            } finally {
                graphics.dispose();
            }
            writeJpeg(normalized, target);
        } finally {
            source.flush();
            normalized.flush();
        }
    }

    private BufferedImage readImage(MultipartFile file) {
        try (InputStream input = file.getInputStream();
             ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) throw new BadRequestException("Не удалось прочитать изображение");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new BadRequestException("Для аватара используйте изображение JPEG или PNG");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_SOURCE_PIXELS) {
                    throw new BadRequestException("Разрешение изображения слишком большое");
                }
                BufferedImage image = reader.read(0);
                if (image == null) throw new BadRequestException("Не удалось прочитать изображение");
                return image;
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new BadRequestException("Не удалось прочитать изображение");
        }
    }

    private void writeJpeg(BufferedImage image, Path target) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) throw new IllegalStateException("JPEG writer не доступен");
        ImageWriter writer = writers.next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(Files.newOutputStream(target))) {
            if (output == null) throw new IOException("Не удалось создать выходной поток");
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(JPEG_QUALITY);
            writer.setOutput(output);
            writer.write(null, new IIOImage(image, null, null), params);
        } catch (IOException e) {
            try { Files.deleteIfExists(target); } catch (IOException ignored) { /* основное исключение важнее */ }
            throw new RuntimeException("Не удалось сохранить обработанный аватар", e);
        } finally {
            writer.dispose();
        }
    }
}
