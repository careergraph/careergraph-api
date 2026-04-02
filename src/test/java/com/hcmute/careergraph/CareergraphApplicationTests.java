package com.hcmute.careergraph;

import com.hcmute.careergraph.repositories.CandidateESRepository;
import com.hcmute.careergraph.repositories.JobESRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CareergraphApplicationTests {

	@MockBean
	private JobESRepository jobESRepository;

	@MockBean
	private CandidateESRepository candidateESRepository;

	@MockBean
	private ChatModel chatModel;

	@MockBean
	private EmbeddingModel embeddingModel;

	@Test
	void contextLoads() {
	}

}
