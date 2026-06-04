package com.hcmute.careergraph.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event phát ra khi File (CV/Resume) được thay đổi
 * Trigger candidate ES re-sync
 */
@Getter
public class FileChangedEvent extends ApplicationEvent {
  
  private final String candidateId;
  private final String fileId;
  private final String fileName;
  private final ChangeType changeType;
  
  public enum ChangeType {
    CREATED,
    UPDATED,
    DELETED
  }
  
  public FileChangedEvent(Object source, String candidateId, String fileId, 
                         String fileName, ChangeType changeType) {
    super(source);
    this.candidateId = candidateId;
    this.fileId = fileId;
    this.fileName = fileName;
    this.changeType = changeType;
  }
}
