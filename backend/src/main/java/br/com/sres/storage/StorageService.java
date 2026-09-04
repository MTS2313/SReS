package br.com.sres.storage;

import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.ListObjectsArgs;
import io.minio.messages.Item;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.BucketExistsArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Service
public class StorageService {
    private static final Logger log = LoggerFactory.getLogger(StorageService.class);
    private final MinioClient client;
    private final String bucket;

    public StorageService(@Value("${sres.storage.endpoint:http://localhost:9000}") String endpoint,
                          @Value("${sres.storage.access-key:minio_dev}") String accessKey,
                          @Value("${sres.storage.secret-key:change-me-development-only}") String secretKey,
                          @Value("${sres.storage.bucket:sres-reports}") String bucket) {
        this.client = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
        this.bucket = bucket;
    }

    public String putTemporary(byte[] content, String contentType) {
        ensureBucket();
        String key = "tmp/" + UUID.randomUUID();
        try {
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .contentType(contentType).build());
            return key;
        } catch (Exception exception) {
            throw failure("Falha ao armazenar arquivo temporário", exception);
        }
    }

    public String promote(String temporaryKey, UUID accountId, UUID reportId) {
        String finalKey = "accounts/" + accountId + "/reports/" + reportId + "/input.pdf";
        try {
            client.copyObject(CopyObjectArgs.builder().bucket(bucket).object(finalKey)
                    .source(CopySource.builder().bucket(bucket).object(temporaryKey).build()).build());
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(temporaryKey).build());
            return finalKey;
        } catch (Exception exception) {
            throw failure("Falha ao promover arquivo para o relatório", exception);
        }
    }

    public String putOutput(byte[] content, UUID accountId, UUID reportId) {
        String key = "accounts/" + accountId + "/reports/" + reportId + "/output.md";
        try {
            ensureBucket();
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(key).contentType("text/markdown")
                    .stream(new ByteArrayInputStream(content), content.length, -1).build());
            return key;
        } catch (Exception exception) {
            throw new StorageException("Falha ao armazenar resultado", exception);
        }
    }

    public GetObjectResponse get(String objectKey) {
        try {
            return client.getObject(GetObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception exception) {
            throw failure("Falha ao ler arquivo", exception);
        }
    }

    public void delete(String objectKey) {
        if (objectKey == null) return;
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception exception) {
            throw failure("Falha ao remover arquivo", exception);
        }
    }

    public String bucket() { return bucket; }

    @Scheduled(initialDelayString = "${sres.storage.cleanup-interval-ms:3600000}",
            fixedDelayString = "${sres.storage.cleanup-interval-ms:3600000}")
    public void cleanupExpiredTemporaryObjects() {
        cleanupExpiredTemporaryObjects(java.time.Instant.now().minus(java.time.Duration.ofHours(1)));
    }

    public void cleanupExpiredTemporaryObjects(java.time.Instant cutoff) {
        try {
            Iterable<io.minio.Result<Item>> objects = client.listObjects(ListObjectsArgs.builder()
                    .bucket(bucket).prefix("tmp/").recursive(true).build());
            for (var result : objects) {
                Item item = result.get();
                if (item.lastModified().toInstant().isBefore(cutoff)) delete(item.objectName());
            }
        } catch (Exception exception) {
            log.warn("Não foi possível concluir a limpeza de temporários do MinIO ({}).", exception.getClass().getSimpleName());
        }
    }

    private void ensureBucket() {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception exception) {
            throw failure("Falha ao preparar bucket privado", exception);
        }
    }

    private static StorageException failure(String message, Exception cause) {
        return new StorageException(message, cause);
    }

    public static class StorageException extends RuntimeException {
        public StorageException(String message, Throwable cause) { super(message, cause); }
    }
}
