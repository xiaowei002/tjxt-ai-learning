package com.tianji.aigc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/embedding")
@Slf4j
@RequiredArgsConstructor
public class EmbeddingController {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    /**
     * 保存文本到向量库
     *
     * @param messages
     */
    @PostMapping
    public void saveVectorStore(@RequestParam("messages") List<String> messages) {
        log.info("保存到向量数据库中，消息数据：{}", messages);
        //message转换为Document
        List<Document> list = messages.stream().map(message -> Document.builder()
                .text(message)
                .build()).toList();
        //保存到向量数据库（向量化）
        vectorStore.add(list);
        log.info("保存到向量数据库成功，数据条数：{}", list.size());
    }

    /**
     * 文本转向量
     *
     * @param message
     * @return
     */
    @GetMapping
    public EmbeddingResponse embed(@RequestParam("message") String message) {
        return this.embeddingModel.embedForResponse(List.of(message));
    }

    /**
     * 删除向量库中的数据
     *
     * @param ids
     */
    @DeleteMapping
    public void deleteVectorStore(@RequestParam("ids") List<String> ids) {
        vectorStore.delete(ids);
    }

    /**
     * 搜索数据，topK = 5
     *
     * @param message
     * @return
     */
    @GetMapping("/search")
    public List<Document> search(@RequestParam("message") String message) {
        return this.vectorStore.similaritySearch(SearchRequest.builder().query(message).topK(5).build());
    }

    /**
     * 搜索全部数据，topK 默认DEFAULT_TOP_K = 4
     * @return
     */
    @GetMapping("/search/all")
    public List<Document> searchAll() {
        // 搜索全部数据
        return this.vectorStore.similaritySearch(SearchRequest.builder().query("").topK(999).build());
    }
}
