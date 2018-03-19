package fi.vm.sade.sharedutils;

import org.jasig.cas.client.authentication.SimplePrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

public class FakeAuthenticationInitialiser {
    private static Logger LOG = LoggerFactory.getLogger(FakeAuthenticationInitialiser.class);
    public static void fakeAuthentication() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_GLOBAL);
        SecurityContextImpl context = new SecurityContextImpl();
        context.setAuthentication(new TestingAuthenticationToken(new SimplePrincipal("1.2.246.562.24.64735725450"), new Object()));
        SecurityContextHolder.setContext(context);
    }

    public FakeAuthenticationInitialiser() {
        LOG.warn("Initialising fake authentication");
        fakeAuthentication();
    }
}
