package com.PetStore.productservice.assistant;

import com.PetStore.product.model.Product;
import com.PetStore.product.repository.ProductRepository;
import dev.langchain4j.service.AiServices;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PetStoreAssistantImpl {
    
    private final PetStoreAssistant assistant;

    public PetStoreAssistantImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
        this.assistant = AiServices.builder(PetStoreAssistant.class)
                .build();
    }



    public String chat(String userMessage) {
        return assistant.chat(userMessage);
    }
}