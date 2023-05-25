package com.sp.aop_demo.rest;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@CrossOrigin(origins = "*")
public class FileUploadController {

	private static final String UPLOAD_FOLDER = "D:/input";

	@PostMapping("/api/files/upload")
	public ResponseEntity<UploadStatusResponse> uploadFile(@RequestParam("issueType") String issueType,
			@RequestParam("panNo") String panNo, @RequestParam("file") MultipartFile file) {
		try {
			log.info("start exec");
			// FTP server details
			String serverIp = "172.12.1.12";
			int serverPort = 22;
			String username = "vmuser";
			String password = "ok1234";

			// Save the file locally
			String fileName = file.getOriginalFilename();
			assert fileName != null;
			Path uploadPath = Paths.get(UPLOAD_FOLDER, fileName);
			File localFile = uploadPath.toFile();
			file.transferTo(localFile);

			// Establish an FTP connection using jcraft jsch
			JSch jSch = new JSch();
			log.info("load config");
			Session session = jSch.getSession(username, serverIp, serverPort);
			session.setConfig("StrictHostKeyChecking", "no");
			session.setPassword(password);
			log.info("stat connection");
			try {
				session.connect();
			} catch (JSchException e) {
				if (isConnectionTimeoutException(e)) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new UploadStatusResponse(issueType, panNo, "Connection timed out. Failed to connect to the FTP server."));
				} else {
					throw e;
				}
			}
			log.info("connection success");

			ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
			channel.connect();
			log.info("file sent start");
			// Upload the file to the FTP server
			channel.put(file.getInputStream(), file.getOriginalFilename());
			log.info("file sent successs");
			// Close the FTP connection
			channel.disconnect();
			session.disconnect();
			log.info("disconnect resources");
			// Create the response object
			UploadStatusResponse response = new UploadStatusResponse(issueType, panNo, "File uploaded successfully!");

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	private boolean isConnectionTimeoutException(JSchException e) {
		return e.getMessage().toLowerCase().contains("connection timed out");
	}

	// Response class for upload status
	private static class UploadStatusResponse {
		private final String issueType;
		private final String panNo;
		private final String uploadStatus;

		public UploadStatusResponse(String issueType, String panNo, String uploadStatus) {
			this.issueType = issueType;
			this.panNo = panNo;
			this.uploadStatus = uploadStatus;
		}

		public String getIssueType() {
			return issueType;
		}

		public String getPanNo() {
			return panNo;
		}

		public String getUploadStatus() {
			return uploadStatus;
		}
	}
}
