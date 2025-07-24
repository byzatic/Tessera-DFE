package io.github.byzatic.tessera.engine.infrastructure.service.service_manager.service_api_interface;

import ru.byzatic.metrics_core.mcg3_enginecommon_lib.logging.MdcContextInterface;
import ru.byzatic.metrics_core.service_lib.execution_context.ExecutionContextInterface;

public class ExecutionContext implements ExecutionContextInterface {
    private MdcContextInterface mdcContextInterface;

    public ExecutionContext(MdcContextInterface mdcContextInterface) {
        this.mdcContextInterface = mdcContextInterface;
    }

    @Override
    public MdcContextInterface getMdcContext() {
        return mdcContextInterface;
    }
}
