// package com.hcmute.careergraph.services.impl;

// import com.hcmute.careergraph.services.QueryEnrichmentService;
// import lombok.RequiredArgsConstructor;
// // import org.springframework.ai.google.genai.GoogleGenAiChatModel;
// import org.springframework.ai.chat.model.ChatModel;
// import org.springframework.stereotype.Service;

// @Service
// @RequiredArgsConstructor
// public class QueryEnrichmentServiceImpl implements QueryEnrichmentService {

//     // private final GoogleGenAiChatModel chatModel;


//     // private final ChatModel chatModel;

//     @Override
//     public String normalizeToEnglish(String input) {

//         String prompt = """
//     You are a language normalization system for job semantic search.

//     Task:
//     - If the input is already in English, return it unchanged.
//     - If the input is NOT in English, translate it into professional English.
//     - Preserve the original semantic meaning.
//     - Do NOT add explanations.

//     Input:
//     %s
//     """.formatted(input);

//         return chatModel
//                 .call(prompt);
// //        return chatModel
// //                .prompt(prompt)
// //                .call()
// //                .content()
// //                .trim();
//     }

// }
