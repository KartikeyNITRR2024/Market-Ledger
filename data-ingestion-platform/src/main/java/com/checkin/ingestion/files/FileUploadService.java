package com.checkin.ingestion.files;

import java.io.IOException;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

	UploadTaskResponseDto accept(MultipartFile file, UUID uploadedBy) throws IOException;

}
