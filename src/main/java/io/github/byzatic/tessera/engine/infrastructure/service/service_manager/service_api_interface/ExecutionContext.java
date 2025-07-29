package io.github.byzatic.tessera.engine.infrastructure.service.service_manager.service_api_interface;

import io.github.byzatic.tessera.enginecommon.logging.MdcContextInterface;
import io.github.byzatic.tessera.service.execution_context.ExecutionContextInterface;

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
