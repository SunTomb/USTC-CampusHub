package com.campushub.file;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileResourceRepository extends JpaRepository<FileResource, Long> {

    @Override
    @EntityGraph(attributePaths = "uploader")
    Optional<FileResource> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "uploader")
    List<FileResource> findAll();
}
