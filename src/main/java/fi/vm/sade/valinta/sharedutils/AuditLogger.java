package fi.vm.sade.valinta.sharedutils;

import fi.vm.sade.auditlog.Logger;
import org.slf4j.LoggerFactory;

public class AuditLogger implements Logger {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuditLogger.class);

    @Override
    public void log(String msg) {
        LOGGER.info(msg);
    }

}
