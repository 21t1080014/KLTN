package com.dungochung.shopdongho.common;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

	@Value("${upload.dir}")
	private String uploadDir;

	public ResponseEntity<Resource> getImageAsResponse(String fileName) {
		try {
			Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
			Resource resource = new UrlResource(filePath.toUri());

			if (!resource.exists() || !resource.isReadable()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
			}

			// You can detect file type instead of hardcoding "image/jpeg" if needed
			String contentType = Files.probeContentType(filePath);
			if (contentType == null) {
				contentType = "application/octet-stream";
			}

			return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, contentType).body(resource);

		} catch (MalformedURLException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	// Upload file mới theo loại (product, brand,...)
	public String saveFile(MultipartFile file, String subFolder) throws IOException {
		if (file == null || file.isEmpty())
			return null;

		String originalFilename = file.getOriginalFilename();
		String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
		String fileName = UUID.randomUUID().toString() + "_" + sanitizedFilename;

		// Tạo đường dẫn thư mục con (ví dụ: D:/uploadImgshop/products)
		File uploadFolder = new File(uploadDir, subFolder);
		if (!uploadFolder.exists()) {
			uploadFolder.mkdirs();
		}

		File destinationFile = new File(uploadFolder, fileName);
		file.transferTo(destinationFile);

		return fileName; // trả về đường dẫn tương đối để lưu DB
	}

	// Xoá file (theo đường dẫn tương đối: products/abc.jpg)
	// Xoá file (theo subfolder và tên file)
	public boolean deleteFile(String subFolder, String filename) {
		if (filename == null || filename.isEmpty())
			return false;

		File file = new File(uploadDir, subFolder + "/" + filename);
		return file.exists() && file.delete();
	}

	// Cập nhật file cũ bằng file mới
	public String updateFile(String oldRelativePath, MultipartFile newFile, String subFolder) throws IOException {
		if (oldRelativePath != null) {
			deleteFile(subFolder,oldRelativePath);
		}
		return saveFile(newFile, subFolder);
	}

	// Kiểm tra tồn tại
	public boolean fileExists(String relativePath) {
		return Files.exists(Paths.get(uploadDir, relativePath));
	}
}
