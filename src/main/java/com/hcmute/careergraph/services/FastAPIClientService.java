package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.request.ChatRequest;
import com.hcmute.careergraph.persistence.dtos.response.ChatResponse;

public interface FastAPIClientService {

    ChatResponse chat(ChatRequest request);
}
