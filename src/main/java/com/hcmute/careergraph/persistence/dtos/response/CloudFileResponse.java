package com.hcmute.careergraph.persistence.dtos.response;

import com.hcmute.careergraph.enums.common.FileType;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class CloudFileResponse {

    @Setter
    private String publicId;
    @Setter
    private String url;
    @Setter
    private String secureUrl;
    @Setter
    private String format;
    @Setter
    private String resourceType;
    @Setter
    private Long bytes;
    private List<String> tags = new ArrayList<>();
    @Setter
    private String ownerType;
    @Setter
    private String ownerId;
    @Setter
    private FileType fileType;
    @Setter
    private Date uploadedAt;

    public CloudFileResponse() {}

    public String getPublicId() { return publicId; }

    public String getUrl() { return url; }

    public String getSecureUrl() { return secureUrl; }

    public String getFormat() { return format; }

    public String getResourceType() { return resourceType; }

    public Long getBytes() { return bytes; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags == null ? new ArrayList<>() : tags; }

    public String getOwnerType() { return ownerType; }

    public String getOwnerId() { return ownerId; }

    public FileType getFileType() { return fileType; }

    public Date getUploadedAt() { return uploadedAt; }

    @Override
    public String toString() {
        return "CloudFileResponse{" +
                "publicId='" + publicId + '\'' +
                ", secureUrl='" + secureUrl + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", fileType='" + fileType + '\'' +
                ", ownerType='" + ownerType + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", bytes=" + bytes +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CloudFileResponse that = (CloudFileResponse) o;
        return Objects.equals(publicId, that.publicId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicId);
    }
}
