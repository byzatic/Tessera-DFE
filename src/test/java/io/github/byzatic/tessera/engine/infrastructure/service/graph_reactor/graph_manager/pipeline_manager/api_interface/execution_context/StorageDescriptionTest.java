package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface.execution_context;

import io.github.byzatic.tessera.workflowroutine.execution_context.StorageOptionInterface;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class StorageDescriptionTest {

    @Test
    public void shouldExposeValuesProvidedByBuilder() {
        List<StorageOptionInterface> options = Collections.singletonList(
                StorageOption.newBuilder()
                        .setKey("retention")
                        .setValue("30d")
                        .build()
        );

        StorageDescription description = StorageDescription.newBuilder()
                .setIdName("metrics")
                .setDescription("Metrics storage")
                .setOptions(options)
                .build();

        assertEquals("metrics", description.getIdName());
        assertEquals("Metrics storage", description.getDescription());
        assertSame(options, description.getOptions());
    }
}
