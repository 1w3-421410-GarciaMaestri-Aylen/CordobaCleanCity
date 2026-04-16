package com.example.garbagereporting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.garbagereporting.config.ApplicationProperties;
import com.example.garbagereporting.dto.VisionClassificationResultDto;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalCatalogClassificationServiceTest {

    private static final String TRASH_MATCH_MESSAGE =
            "Coincidencia exacta con catalogo local de basura. El reporte fue clasificado como basura.";
    private static final String NON_TRASH_MATCH_MESSAGE =
            "Coincidencia exacta con catalogo local de no basura. El reporte fue clasificado como no basura.";
    private static final String NO_MATCH_MESSAGE =
            "La imagen no coincide con ningun archivo del catalogo local. Se clasifica como no basura por defecto.";

    @TempDir
    Path tempDir;

    @Test
    void classifyReturnsTrashWhenImageHashMatchesTrashCatalog() throws IOException {
        Path trashCatalog = tempDir.resolve("catalog/basura");
        Path nonTrashCatalog = tempDir.resolve("catalog/no-basura");
        LocalCatalogClassificationService service = new LocalCatalogClassificationService(
                buildProperties(trashCatalog, nonTrashCatalog)
        );

        byte[] imageBytes = "exact-trash-image".getBytes(StandardCharsets.UTF_8);
        writeBytes(trashCatalog.resolve("trash-reference.jpg"), imageBytes);
        writeBytes(nonTrashCatalog.resolve("clean-reference.jpg"), "clean-image".getBytes(StandardCharsets.UTF_8));
        Path uploadedImage = writeBytes(tempDir.resolve("uploaded-trash.jpg"), imageBytes);

        VisionClassificationResultDto result = service.classify(uploadedImage.toString(), "uploaded-trash.jpg");

        assertThat(result.isTrash()).isTrue();
        assertThat(result.getClassificationResult()).isEqualTo(TRASH_MATCH_MESSAGE);
        assertThat(result.getConfidence()).isEqualTo(1.0);
    }

    @Test
    void classifyReturnsNonTrashWhenImageHashMatchesNonTrashCatalog() throws IOException {
        Path trashCatalog = tempDir.resolve("catalog/basura");
        Path nonTrashCatalog = tempDir.resolve("catalog/no-basura");
        LocalCatalogClassificationService service = new LocalCatalogClassificationService(
                buildProperties(trashCatalog, nonTrashCatalog)
        );

        byte[] imageBytes = "exact-clean-image".getBytes(StandardCharsets.UTF_8);
        writeBytes(trashCatalog.resolve("trash-reference.jpg"), "other-trash-image".getBytes(StandardCharsets.UTF_8));
        writeBytes(nonTrashCatalog.resolve("clean-reference.jpg"), imageBytes);
        Path uploadedImage = writeBytes(tempDir.resolve("uploaded-clean.jpg"), imageBytes);

        VisionClassificationResultDto result = service.classify(uploadedImage.toString(), "uploaded-clean.jpg");

        assertThat(result.isTrash()).isFalse();
        assertThat(result.getClassificationResult()).isEqualTo(NON_TRASH_MATCH_MESSAGE);
        assertThat(result.getConfidence()).isEqualTo(1.0);
    }

    @Test
    void classifyReturnsDefaultNonTrashWhenImageHashDoesNotMatchCatalogs() throws IOException {
        Path trashCatalog = tempDir.resolve("catalog/basura");
        Path nonTrashCatalog = tempDir.resolve("catalog/no-basura");
        LocalCatalogClassificationService service = new LocalCatalogClassificationService(
                buildProperties(trashCatalog, nonTrashCatalog)
        );

        writeBytes(trashCatalog.resolve("trash-reference.jpg"), "trash-image".getBytes(StandardCharsets.UTF_8));
        writeBytes(nonTrashCatalog.resolve("clean-reference.jpg"), "clean-image".getBytes(StandardCharsets.UTF_8));
        Path uploadedImage = writeBytes(tempDir.resolve("uploaded-unknown.jpg"), "different-image".getBytes(StandardCharsets.UTF_8));

        VisionClassificationResultDto result = service.classify(uploadedImage.toString(), "uploaded-unknown.jpg");

        assertThat(result.isTrash()).isFalse();
        assertThat(result.getClassificationResult()).isEqualTo(NO_MATCH_MESSAGE);
        assertThat(result.getConfidence()).isNull();
    }

    @Test
    void initializeCatalogDirectoriesCreatesMissingFolders() {
        Path trashCatalog = tempDir.resolve("catalog/basura");
        Path nonTrashCatalog = tempDir.resolve("catalog/no-basura");
        LocalCatalogClassificationService service = new LocalCatalogClassificationService(
                buildProperties(trashCatalog, nonTrashCatalog)
        );

        service.initializeCatalogDirectories();

        assertThat(Files.isDirectory(trashCatalog)).isTrue();
        assertThat(Files.isDirectory(nonTrashCatalog)).isTrue();
    }

    private ApplicationProperties buildProperties(Path trashCatalog, Path nonTrashCatalog) {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getVision().setTrashCatalogDir(trashCatalog.toString());
        properties.getVision().setNonTrashCatalogDir(nonTrashCatalog.toString());
        return properties;
    }

    private Path writeBytes(Path outputPath, byte[] data) throws IOException {
        Files.createDirectories(outputPath.getParent());
        return Files.write(outputPath, data);
    }
}
