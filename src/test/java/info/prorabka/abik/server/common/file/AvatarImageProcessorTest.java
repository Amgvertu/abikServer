package info.prorabka.abik.server.common.file;

import info.prorabka.abik.server.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvatarImageProcessorTest {

    private final AvatarImageProcessor processor = new AvatarImageProcessor();

    @TempDir
    Path tempDir;

    @Test
    void normalizesLandscapeToCenteredSquareJpeg() throws IOException {
        BufferedImage source = new BufferedImage(1000, 500, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                Color color = x < 333 ? Color.RED : x < 666 ? Color.GREEN : Color.BLUE;
                source.setRGB(x, y, color.getRGB());
            }
        }
        MockMultipartFile file = imageFile(source, "png", "avatar.png", "image/png");
        Path target = tempDir.resolve("normalized.jpg");

        processor.writeNormalized(file, target);

        BufferedImage saved = ImageIO.read(target.toFile());
        assertEquals(AvatarImageProcessor.AVATAR_SIZE, saved.getWidth());
        assertEquals(AvatarImageProcessor.AVATAR_SIZE, saved.getHeight());
        Color center = new Color(saved.getRGB(256, 256));
        assertTrue(center.getGreen() > center.getRed() && center.getGreen() > center.getBlue());
        assertTrue(Files.size(target) > 0);
    }

    @Test
    void rejectsInvalidImage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3, 4});
        Path target = tempDir.resolve("invalid.jpg");

        assertThrows(BadRequestException.class, () -> processor.writeNormalized(file, target));
    }

    private MockMultipartFile imageFile(BufferedImage image, String format, String name, String mime) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return new MockMultipartFile("file", name, mime, output.toByteArray());
    }
}
