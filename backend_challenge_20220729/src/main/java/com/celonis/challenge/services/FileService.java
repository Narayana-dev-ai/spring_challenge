package com.celonis.challenge.services;

import com.celonis.challenge.exceptions.InternalException;
import com.celonis.challenge.model.ProjectGenerationTask;
import com.celonis.challenge.model.ProjectGenerationTaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class FileService {

    private final TaskService taskService;

    private final ProjectGenerationTaskRepository projectGenerationTaskRepository;

    public FileService(TaskService taskService,
                       ProjectGenerationTaskRepository projectGenerationTaskRepository) {
        this.taskService = taskService;
        this.projectGenerationTaskRepository = projectGenerationTaskRepository;
    }

    public ResponseEntity<FileSystemResource> getTaskResult(String taskId) {
        ProjectGenerationTask projectGenerationTask = taskService.getTask(taskId);
        File inputFile = new File(projectGenerationTask.getStorageLocation());
        System.out.println("Check for path" + projectGenerationTask.getStorageLocation());

        if (!inputFile.exists()) {
            throw new InternalException("File not generated yet");
        }

        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        respHeaders.setContentDispositionFormData("attachment", "challenge.zip");

        return new ResponseEntity<>(new FileSystemResource(inputFile), respHeaders, HttpStatus.OK);
    }

    public void storeResult(String taskId, URL url) throws IOException {
        ProjectGenerationTask projectGenerationTask = taskService.getTask(taskId);
        File outputFile = File.createTempFile(taskId, ".zip");
        outputFile.deleteOnExit();
        projectGenerationTask.setStorageLocation(outputFile.getAbsolutePath());
        projectGenerationTaskRepository.save(projectGenerationTask);

        ObjectMapper mapper = new ObjectMapper();
        String jsonData = mapper.writeValueAsString(projectGenerationTask);

        try (FileOutputStream fos = new FileOutputStream(outputFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            ZipEntry entry = new ZipEntry("data.json");
            zos.putNextEntry(entry);

            byte[] bytes = jsonData.getBytes(StandardCharsets.UTF_8);
            zos.write(bytes);
            zos.closeEntry();
        }
    }
}
