package com.hcmute.careergraph.helper;

import com.hcmute.careergraph.enums.common.FileType;

public class StringHelper {

    public static String buildObjectName(String candidateId, FileType fileType, String originalFile) {

        /*
        * Get extension
        * Ex: jpg, png, pdf, docs,...
        * */
        String extension = "";
        if (originalFile != null && originalFile.contains(".")) {
            extension = originalFile.substring(originalFile.lastIndexOf("."));
        }

        String baseName = switch (fileType) {
            case AVATAR -> candidateId + "-avatar";
            case COVER -> candidateId + "-cover";
            case RESUME -> candidateId + "-resume";
        };

        // Ex: abc.png case avatar -> candidates/123/123-avatar.png
        return String.format("candidates/%s/%s%s", candidateId, baseName, extension);
    }
}
