# Image App API

## Overview
The **Image App API** is a Spring Boot-based RESTful API for managing images stored in AWS S3 and performing label-based image searches using AWS Rekognition. It provides endpoints to upload images, retrieve paginated image lists, and search images based on labels.

## Features
- **Upload Images**: Upload image files to an AWS S3 bucket.
- **List Images**: Retrieve paginated lists of images stored in the S3 bucket.
- **Search Images**: Use AWS Rekognition to search images by labels (e.g., "cat", "car").
- **API Documentation**: Interactive API documentation using Swagger/OpenAPI.

## Tech
- **Java**
- **Spring Boot**
- **AWS S3**
- **Amazon Rekognition**
- **REST API**
- **Swagger**
- **Multipart File Upload**
- **Pagination**
- **Image Processing**
- **Cloud Storage**
- **Microservices**

## API Endpoints

### Upload Image

`POST /api/images/upload`

- **Description**: Uploads an image file to the S3 bucket.
- **Parameters**:
    - `image` (Multipart File, Required): The image file to upload. Supported formats: `.jpg`, `.jpeg`, `.png`.
- **Responses**:
    - `200 OK`: Image uploaded successfully.
    - `500 Internal Server Error`: Failure due to file read issues or S3 errors.

---

### List Images

`GET /api/images/get`

- **Description**: Retrieves a paginated list of image URLs stored in the S3 bucket.
- **Query Parameters**:
    - `page` (Integer, Optional, Default: `1`): Page number for pagination.
    - `pageSize` (Integer, Optional, Default: `10`): Number of images per page.
- **Responses**:
    - `200 OK`:
      ```json
      {
        "images": [
          {"url": "https://bucket.s3.region.amazonaws.com/image1.jpg"},
          {"url": "https://bucket.s3.region.amazonaws.com/image2.jpg"}
        ],
        "currentPage": 1,
        "pageSize": 10,
        "totalImages": 50,
        "nextPageToken": "abc123"
      }
      ```
    - `500 Internal Server Error`: Error while fetching images from S3.

---

### Search Images

`GET /api/images/search`

- **Description**: Searches images in the S3 bucket based on labels using AWS Rekognition.
- **Query Parameters**:
    - `query` (String, Required): The label to search for (e.g., "cat", "car").
- **Responses**:
    - `200 OK`:
      ```json
      [
        {"url": "https://bucket.s3.region.amazonaws.com/image1.jpg"},
        {"url": "https://bucket.s3.region.amazonaws.com/image2.jpg"}
      ]
      ```
    - `500 Internal Server Error`: Error during label detection or S3 access.
