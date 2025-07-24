package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.byzatic.commons.ObjectsUtils;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.service.GraphManagerInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.GraphTraversal;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.GraphTraversalInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.node_repository.GraphManagerNodeRepositoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.sheduller.JobDetail;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.sheduller.SchedulerInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.sheduller.health.HealthStateProxy;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.sheduller.job.Job;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.PipelineManagerFactoryInterface;
import io.github.byzatic.tessera.engine.domain.repository.storage.StorageManagerInterface;

import java.util.ArrayList;
import java.util.List;

public class GraphManager implements GraphManagerInterface {
    private final static Logger logger= LoggerFactory.getLogger(GraphManager.class);
    private final PipelineManagerFactoryInterface pipelineManagerFactory;
    private final StorageManagerInterface storageManager;
    private SchedulerInterface scheduler = null;
    private GraphManagerNodeRepositoryInterface graphManagerNodeRepository = null;

    public GraphManager(@NotNull GraphManagerNodeRepositoryInterface graphManagerNodeRepository, SchedulerInterface scheduler, PipelineManagerFactoryInterface pipelineManagerFactory, StorageManagerInterface storageManager) {
        ObjectsUtils.requireNonNull(graphManagerNodeRepository, new IllegalArgumentException(GraphManagerNodeRepositoryInterface.class.getSimpleName() + " should be NotNull"));
        ObjectsUtils.requireNonNull(scheduler, new IllegalArgumentException(SchedulerInterface.class.getSimpleName() + " should be NotNull"));
        this.graphManagerNodeRepository = graphManagerNodeRepository;
        this.scheduler = scheduler;
        this.pipelineManagerFactory = pipelineManagerFactory;
        this.storageManager = storageManager;
    }

    @Override
    public void runGraph() throws OperationIncompleteException {
        Boolean passProcessGraph = Boolean.FALSE;

        // get roots
        @NotNull List<GraphNodeRef> rootGraphNodeRefList = this.graphManagerNodeRepository.getRootNodes();
        if (rootGraphNodeRefList.isEmpty()) {
            passProcessGraph = Boolean.TRUE;
        }
        logger.debug("Pass graph processing -> {}", passProcessGraph);

        if (! passProcessGraph) {

            for (JobDetail jobDetail : generateJobDetails(rootGraphNodeRefList)) {
                this.scheduler.addJob(jobDetail);
            }
            logger.debug("Job details prepared");

            this.scheduler.runAllJobs();
            logger.debug("Scheduler complete");

            this.scheduler.cleanup();
            logger.debug("Scheduler cleanup complete");

            this.storageManager.cleanupNodeStorages();
            logger.debug("StorageManager cleanup complete");

            this.graphManagerNodeRepository.clearNodeStatuses();
            logger.debug("GraphManagerNodeRepository cleanup complete");

        } else {
            logger.warn("Graph Roots was not found; pass graph processing!");
        }
    }

    @NotNull
    private List<JobDetail> generateJobDetails(@NotNull List<GraphNodeRef> rootGraphNodeRefList) throws OperationIncompleteException {
        try {
            logger.debug("Root jpa_like_node_repository ref list -> {}", rootGraphNodeRefList);
            List<JobDetail> jobDetails = new ArrayList<>();
            for (GraphNodeRef rootNodeGraphNodeRef : rootGraphNodeRefList) {
                HealthStateProxy healthStateProxy = HealthStateProxy.newBuilder().build();
                GraphTraversalInterface graphTraversal = new GraphTraversal(
                        graphManagerNodeRepository,
                        pipelineManagerFactory
                );
                Job job = new Job(
                        graphTraversal,
                        graphManagerNodeRepository.getNode(rootNodeGraphNodeRef),
                        healthStateProxy
                );
                JobDetail rootNodeJobDetail = JobDetail.newBuilder()
                        .job(job)
                        .HealthStateProxy(healthStateProxy)
                        .build();
                jobDetails.add(rootNodeJobDetail);
            }
            logger.debug("Job details list -> {}", rootGraphNodeRefList);
            return jobDetails;
        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
    }





}
