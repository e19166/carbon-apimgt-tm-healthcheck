/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.tm.healthcheck;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.healthcheck.api.core.AbstractHealthChecker;
import org.wso2.carbon.healthcheck.api.core.exception.BadHealthException;
import org.wso2.carbon.healthcheck.api.core.model.HealthCheckerConfig;

import java.util.Properties;

/**
 * Reports whether the Traffic Manager can reach, and actually use, the Control Plane on every connection
 * its Event Hub dependency relies on: the throttling-data connections (9611, 9711), the message broker
 * connections (5672, and 8672 when TLS messaging is configured), and the management/REST connection.
 *
 * <p>Registered as an OSGi {@code HealthChecker} service by
 * {@link org.wso2.carbon.apimgt.tm.healthcheck.internal.TMHealthCheckServiceComponent} and picked up
 * automatically by the existing, unmodified carbon-health-check {@code GET /health} endpoint - see
 * {@code contracts/health-checker-spi.md} for the full behavioral contract this class implements.</p>
 *
 * <p><b>Status</b>: this class is currently the Phase 2 (Foundational) skeleton only - {@link #getName()}
 * and {@link #init(HealthCheckerConfig)} are complete, but {@link #checkHealth()} has no real
 * connectivity-check logic yet. That logic (the two-stage port-responsiveness and accept-confirmation
 * checks) is added in Phase 3, User Story 1 (tasks T009-T014).</p>
 */
public class TMControlPlaneConnectivityHealthChecker extends AbstractHealthChecker {

    private static final Log log = LogFactory.getLog(TMControlPlaneConnectivityHealthChecker.class);

    /**
     * The fixed name this checker registers under. This MUST match the {@code name} attribute of this
     * checker's {@code <HealthChecker>} block in {@code health-check-config.xml} exactly, since that is
     * the join key {@code HealthCheckConfigParser} uses to bind the XML configuration (timeout, enabled,
     * order) to this particular service instance.
     */
    private static final String CHECKER_NAME = "TMControlPlaneConnectivityHealthChecker";

    /**
     * The {@code <Property>} key, under this checker's own {@code <HealthChecker>} block, that configures
     * the bounded per-connection connect timeout.
     */
    private static final String CONNECT_TIMEOUT_MS_PROPERTY = "connect.timeout.ms";

    /**
     * Default connect timeout, in milliseconds, used when {@link #CONNECT_TIMEOUT_MS_PROPERTY} is absent
     * or malformed in configuration.
     */
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 3000;

    private int connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;

    @Override
    public String getName() {

        return CHECKER_NAME;
    }

    @Override
    public void init(HealthCheckerConfig healthCheckerConfig) {

        super.init(healthCheckerConfig);
        connectTimeoutMs = readConnectTimeoutMs(healthCheckerConfig);
    }

    /**
     * Reads {@link #CONNECT_TIMEOUT_MS_PROPERTY} out of the given configuration, falling back to
     * {@link #DEFAULT_CONNECT_TIMEOUT_MS} (with a log message) when the property is absent or not a
     * valid integer - mirroring the fallback-with-log pattern carbon-health-check's own
     * {@code DataSourceHealthChecker.setHealthyPoolLimit()} uses for a malformed value.
     *
     * @param healthCheckerConfig this checker's parsed configuration
     * @return the connect timeout to use, in milliseconds
     */
    private int readConnectTimeoutMs(HealthCheckerConfig healthCheckerConfig) {

        Properties properties = healthCheckerConfig.getProperties();
        String configuredValue = properties == null ? null : properties.getProperty(CONNECT_TIMEOUT_MS_PROPERTY);
        if (configuredValue == null) {
            return DEFAULT_CONNECT_TIMEOUT_MS;
        }
        try {
            return Integer.parseInt(configuredValue.trim());
        } catch (NumberFormatException e) {
            log.info("Invalid value configured for '" + CONNECT_TIMEOUT_MS_PROPERTY + "': '" + configuredValue
                    + "' - falling back to the default of " + DEFAULT_CONNECT_TIMEOUT_MS + "ms");
            return DEFAULT_CONNECT_TIMEOUT_MS;
        }
    }

    /**
     * Returns the bounded per-connection connect timeout, in milliseconds, that every port-responsiveness
     * and accept-confirmation check must honour (FR-006). This bound is an internal safety value only and
     * must never itself appear in the response returned by {@link #checkHealth()} (FR-013).
     *
     * @return the configured connect timeout in milliseconds
     */
    protected int getConnectTimeoutMs() {

        return connectTimeoutMs;
    }

    @Override
    public Properties checkHealth() throws BadHealthException {

        // Placeholder only - the real two-stage connectivity check (port responsiveness, then accept
        // confirmation, per connection) is implemented in Phase 3, User Story 1 (tasks T009-T014).
        // Returning an empty Properties bag means this checker is registered and reachable via
        // GET /health without error, but does not yet contribute any connection status.
        return new Properties();
    }
}
