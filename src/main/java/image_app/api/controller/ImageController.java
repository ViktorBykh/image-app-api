package image_app.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.RekognitionException;
import software.amazon.awssdk.services.rekognition.model.InvalidImageFormatException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final S3Client s3Client;
    private final RekognitionClient rekognitionClient;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    public ImageController(S3Client s3Client, RekognitionClient rekognitionClient) {
        this.s3Client = s3Client;
        this.rekognitionClient = rekognitionClient;
    }

    @GetMapping("/get")
    public ResponseEntity<Map<String, Object>> getImages(@RequestParam(value = "page", defaultValue = "1") int page,
                                                         @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        Map<String, Object> responseMap = new HashMap<>();
        List<Map<String, String>> pageImages = new ArrayList<>();
        int startIndex = (page - 1) * pageSize;
        String continuationToken = null;
        int totalImages;
        try {
            List<S3Object> allObjects = new ArrayList<>();
            do {
                ListObjectsV2Request request = ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .maxKeys(pageSize + startIndex)
                        .continuationToken(continuationToken)
                        .build();
                ListObjectsV2Response response = s3Client.listObjectsV2(request);
                totalImages = response.keyCount();
                continuationToken = response.nextContinuationToken();
                allObjects.addAll(response.contents());
            } while (continuationToken != null);
            allObjects.sort(Comparator.comparing(S3Object::lastModified).reversed());
            int start = Math.min(startIndex, allObjects.size());
            int end = Math.min(start + pageSize, allObjects.size());
            for (int i = start; i < end; i++) {
                String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, allObjects.get(i).key());
                pageImages.add(Map.of("url", imageUrl));
            }
            responseMap.put("images", pageImages);
            responseMap.put("currentPage", page);
            responseMap.put("pageSize", pageSize);
            responseMap.put("totalImages", totalImages);
            responseMap.put("nextPageToken", continuationToken);
            return ResponseEntity.ok(responseMap);
        } catch (S3Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch images from S3: " + e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(@RequestParam("image") MultipartFile file) {
        String key = file.getOriginalFilename();
        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build(),
                    RequestBody.fromInputStream(inputStream, file.getSize())
            );

            return ResponseEntity.ok("Image uploaded successfully.");
        } catch (IOException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to read file: " + e.getMessage());
        } catch (S3Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Image upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, String>>> searchImages(@RequestParam("query") String query) {
        List<Map<String, String>> pageImages = new ArrayList<>();
        try {
            ListObjectsV2Response response = s3Client.listObjectsV2(builder -> builder.bucket(bucketName).build());
            response.contents().stream()
                    .map(S3Object::key)
                    .filter(key -> key.toLowerCase().matches(".*\\.(jpg|jpeg|png)$"))
                    .forEach(key -> processImageForMatch(query, key, pageImages));

            return ResponseEntity.ok(pageImages);
        } catch (RekognitionException e) {
            System.err.println("Rekognition error: " + e.awsErrorDetails().errorMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private void processImageForMatch(String query, String key, List<Map<String, String>>  matchedImageUrls) {
        DetectLabelsRequest request = DetectLabelsRequest.builder()
                .image(Image.builder()
                        .s3Object(software.amazon.awssdk.services.rekognition.model.S3Object.builder()
                                .bucket(bucketName)
                                .name(key)
                                .build())
                        .build())
                .maxLabels(10)
                .build();

        try {
            DetectLabelsResponse response = rekognitionClient.detectLabels(request);
            boolean isMatch = response.labels().stream()
                    .anyMatch(label -> label.name().equalsIgnoreCase(query));

            if (isMatch) {
                String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
                matchedImageUrls.add(Map.of("url", imageUrl));
            }
        } catch (InvalidImageFormatException e) {
            System.err.println("Invalid image format: " + key);
        }
    }
}