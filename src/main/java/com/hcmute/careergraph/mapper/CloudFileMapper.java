package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.persistence.dtos.response.CloudFileResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Utility to map Cloudinary API resource maps into CloudFileResponse.
 *
 * Usage:
 *   CloudFileResponse resp = CloudFileMapper.map(resourceMap, "candidates", "123", null);
 *   // or for list:
 *   List<CloudFileResponse> list = CloudFileMapper.mapList(resources, "candidates", "123", null);
 */
public final class CloudFileMapper {

    private static final SimpleDateFormat[] DATE_FORMATS = new SimpleDateFormat[] {
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    };

    private CloudFileMapper() {}

    /**
     * Map a single Cloudinary resource map to CloudFileResponse.
     *
     * @param resource   Cloudinary resource (Map<String,Object>)
     * @param ownerType  ownerType used when listing (e.g. "candidates")
     * @param ownerId    ownerId used when listing (e.g. "123")
     * @param forcedFileType  optional: if provided, set response.fileType to this value,
     *                        otherwise try to derive from public_id path
     */
    @SuppressWarnings("unchecked")
    public static CloudFileResponse map(Map<String, Object> resource, String ownerType, String ownerId, FileType forcedFileType) {
        if (resource == null) return null;

        CloudFileResponse r = new CloudFileResponse();
        r.setOwnerType(ownerType);
        r.setOwnerId(ownerId);

        String publicId = Objects.toString(resource.get("public_id"), null);
        r.setPublicId(publicId);

        r.setUrl(Objects.toString(resource.get("url"), null));
        r.setSecureUrl(Objects.toString(resource.get("secure_url"), null));
        r.setFormat(Objects.toString(resource.get("format"), null));
        r.setResourceType(Objects.toString(resource.get("resource_type"), null));

        Object bytesObj = resource.get("bytes");
        if (bytesObj instanceof Number) {
            r.setBytes(((Number) bytesObj).longValue());
        } else {
            r.setBytes(null);
        }

        List<String> tags = new ArrayList<>();
        Object tagsObj = resource.get("tags");
        if (tagsObj instanceof List) {
            for (Object o : (List<?>) tagsObj) {
                if (o != null) tags.add(o.toString());
            }
        }
        r.setTags(tags);

        // uploadedAt: Cloudinary may return "created_at"
        Object createdAt = resource.get("created_at");
        if (createdAt != null) {
            Date dt = parseDate(createdAt.toString());
            r.setUploadedAt(dt);
        }

        // Determine fileType:
        if (forcedFileType != null && !forcedFileType.name().isBlank()) {
            r.setFileType(forcedFileType);
        } else {
            r.setFileType(guessFileTypeFromPublicId(publicId, ownerType, ownerId));
        }

        return r;
    }

    /**
     * Map a list of resource maps to a list of responses.
     */
    public static List<CloudFileResponse> mapList(List<Map<String, Object>> resources, String ownerType, String ownerId, FileType forcedFileType) {
        if (resources == null) return Collections.emptyList();
        List<CloudFileResponse> list = new ArrayList<>(resources.size());
        for (Map<String, Object> res : resources) {
            CloudFileResponse m = map(res, ownerType, ownerId, forcedFileType);
            if (m != null) list.add(m);
        }
        return list;
    }

    /**
     * Guess fileType by parsing publicId using expected path structure:
     * {ownerType}/{ownerId}/{fileType}/{public_id_base}
     * If cannot guess, returns null.
     */
    private static FileType guessFileTypeFromPublicId(String publicId, String ownerType, String ownerId) {
        if (publicId == null) return null;
        // Guard against null ownerType/ownerId
        String prefix = (ownerType == null || ownerId == null) ? null : String.join("/", ownerType, ownerId) + "/";
        if (prefix != null && publicId.startsWith(prefix)) {
            String remainder = publicId.substring(prefix.length());
            String[] parts = remainder.split("/");
            if (parts.length >= 2) {
                // parts[0] is fileType, try to convert to enum
                return parseFileType(parts[0]);
            } else {
                // if only filename exists, we cannot be sure; return null
                return null;
            }
        } else {
            // fallback: attempt to extract third segment in path (ownerType/ownerId/fileType/...)
            String[] fullParts = publicId.split("/");
            if (fullParts.length >= 3) {
                return parseFileType(fullParts[2]);
            }
        }
        return null;
    }

    private static FileType parseFileType(String part) {
        if (part == null) return null;
        try {
            return FileType.valueOf(part.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            // Unknown value — treat as not determinable
            return null;
        }
    }

    private static Date parseDate(String s) {
        for (SimpleDateFormat fmt : DATE_FORMATS) {
            try {
                fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
                return fmt.parse(s);
            } catch (ParseException ignored) {}
        }
        return null;
    }
}