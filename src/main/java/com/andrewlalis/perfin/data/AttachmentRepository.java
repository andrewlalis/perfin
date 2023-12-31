package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.model.Attachment;

import java.nio.file.Path;
import java.util.Optional;

public interface AttachmentRepository extends AutoCloseable {
    Attachment insert(Path sourcePath);
    Optional<Attachment> findById(long attachmentId);
    Optional<Attachment> findByIdentifier(String identifier);
    void deleteById(long attachmentId);
}
