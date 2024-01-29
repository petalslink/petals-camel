/**
 * Copyright (c) 2015-2024 Linagora
 * 
 * This program/library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 2.1 of the License, or (at your
 * option) any later version.
 * 
 * This program/library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program/library; If not, see http://www.gnu.org/licenses/
 * for the GNU Lesser General Public License version 2.1.
 */
package org.ow2.petals.se.camel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.xml.namespace.QName;

import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfOperation;
import org.ow2.petals.commons.log.PetalsExecutionContext;
import org.ow2.petals.component.framework.api.Constants;
import org.ow2.petals.component.framework.jbidescriptor.generated.MEPType;
import org.ow2.petals.component.framework.junit.JbiConstants;
import org.ow2.petals.component.framework.junit.Message;
import org.ow2.petals.component.framework.junit.RequestMessage;
import org.ow2.petals.component.framework.junit.extensions.ComponentUnderTestExtension;
import org.ow2.petals.component.framework.junit.extensions.api.ComponentUnderTest;
import org.ow2.petals.component.framework.junit.helpers.MessageChecks;
import org.ow2.petals.component.framework.junit.helpers.ServiceProviderImplementation;
import org.ow2.petals.component.framework.junit.helpers.SimpleComponent;
import org.ow2.petals.component.framework.junit.impl.ConsumesServiceConfiguration;
import org.ow2.petals.component.framework.junit.impl.ProvidesServiceConfiguration;
import org.ow2.petals.component.framework.junit.impl.ServiceConfiguration;
import org.ow2.petals.component.framework.junit.impl.message.RequestToProviderMessage;
import org.ow2.petals.component.framework.junit.rule.ServiceConfigurationFactory;
import org.ow2.petals.junit.extensions.log.handler.InMemoryLogHandlerExtension;
import org.ow2.petals.se.camel.utils.JbiCamelConstants;

public abstract class AbstractComponentTest extends AbstractTest implements JbiCamelConstants, JbiConstants {

    public static final URL WSDL11 = Thread.currentThread().getContextClassLoader()
            .getResource("tests/service-1.1.wsdl");

    protected static final URL WSDL20 = Thread.currentThread().getContextClassLoader()
            .getResource("tests/service-2.0.wsdl");

    protected static final URL VALID_ROUTES_11 = Thread.currentThread().getContextClassLoader()
            .getResource("tests/routes-valid-1-1.xml");

    protected static final URL VALID_ROUTES_20 = Thread.currentThread().getContextClassLoader()
            .getResource("tests/routes-valid-2-0.xml");

    protected static final URL INVALID_ROUTES = Thread.currentThread().getContextClassLoader()
            .getResource("tests/routes-invalid.xml");

    protected static final String HELLO_NS = "http://petals.ow2.org";

    protected static final String EXTERNAL_CAMEL_SERVICE_ID = "theConsumesId";

    protected static final String SU_NAME = "su-name";

    public static final QName HELLO_INTERFACE = new QName(HELLO_NS, "HelloInterface");

    public static final QName HELLO_SERVICE = new QName(HELLO_NS, "HelloService");

    public static final QName HELLO_OPERATION = new QName(HELLO_NS, "sayHello");

    public static final QName HELLO_WITHOUT_ECHO_OPERATION = new QName(HELLO_NS, "sayHelloWithoutEcho");

    public static final QName HELLO_WITHOUT_ECHO_ROBUST_OPERATION = new QName(HELLO_NS, "sayHelloWithoutEchoRobust");

    protected static final String HELLO_ENDPOINT = Constants.Component.AUTOGENERATED_ENDPOINT_NAME;

    public static final String EXTERNAL_ENDPOINT_NAME = "externalHelloEndpoint";

    protected static final long DEFAULT_TIMEOUT_FOR_COMPONENT_SEND = 2000;

    @ComponentUnderTestExtension(inMemoryLogHandler = @InMemoryLogHandlerExtension, explicitPostInitialization = true)
    protected static ComponentUnderTest COMPONENT_UNDER_TEST;

    protected static SimpleComponent COMPONENT;
    
    @BeforeAll
    private static void completesComponentUnderTestConfiguration() throws Exception {
        COMPONENT_UNDER_TEST
                // we need faster checks for our tests, 2000 is too long!
                .setParameter(new QName(CDK_NAMESPACE_URI, "time-beetween-async-cleaner-runs"), "100")
                .registerExternalServiceProvider(EXTERNAL_ENDPOINT_NAME, HELLO_SERVICE, HELLO_INTERFACE)
                .postInitComponentUnderTest();
        
        COMPONENT = new SimpleComponent(COMPONENT_UNDER_TEST);
    }

    /**
     * All log traces must be cleared before starting a unit test (because the log handler is static and lives during
     * the whole suite of tests)
     */
    @BeforeEach
    public void clearLogTraces() {
        COMPONENT_UNDER_TEST.getInMemoryLogHandler().clear();
        // we want to clear them inbetween tests
        COMPONENT_UNDER_TEST.clearRequestsFromConsumer();
        COMPONENT_UNDER_TEST.clearResponsesFromProvider();
        // note: incoming messages queue can't be cleared because it is the job of the tested component to well handle
        // any situation
        // JUnit is susceptible to reuse threads apparently
        PetalsExecutionContext.clear();
    }

    /**
     * We undeploy services after each test (because the component is static and lives during the whole suite of tests)
     */
    @AfterEach
    public void after() {

        COMPONENT_UNDER_TEST.undeployAllServices();

        // asserts are ALWAYS a bug!
        final Formatter formatter = new SimpleFormatter();
        for (final LogRecord r : COMPONENT_UNDER_TEST.getInMemoryLogHandler().getAllRecords()) {
            assertFalse(r.getThrown() instanceof AssertionError
                    || r.getMessage().contains("AssertionError"), "Got a log with an assertion: " + formatter.format(r));
        }
    }

    public static ConsumesServiceConfiguration createHelloConsumes() {
        final ConsumesServiceConfiguration consumes = new ConsumesServiceConfiguration(HELLO_INTERFACE, HELLO_SERVICE,
                EXTERNAL_ENDPOINT_NAME);
        consumes.setOperation(HELLO_OPERATION);
        consumes.setMEP(MEPType.IN_OUT);
        // let's use a smaller timeout time by default
        consumes.setTimeout(DEFAULT_TIMEOUT_FOR_COMPONENT_SEND);
        consumes.setParameter(new QName(CAMEL_JBI_NS_URI, EL_CONSUMES_SERVICE_ID), EXTERNAL_CAMEL_SERVICE_ID);
        return consumes;
    }

    protected static ServiceConfigurationFactory createHelloService(final URL wsdl, final @Nullable Class<?> clazz,
            final @Nullable URL routes) throws Exception {

        final ProvidesServiceConfiguration provides = createHelloServiceProvider(wsdl, clazz, routes);

        provides.addServiceConfigurationDependency(createHelloConsumes());

        return new ServiceConfigurationFactory() {
            @Override
            public ServiceConfiguration create() {
                return provides;
            }
        };
    }

    /**
     * Create the configuration of a service provider implemented with the given Camel routes.
     * 
     * @param wsdl
     *            WSDL of the service provider.
     * @param routesClazz
     *            Route definition as Java class. Can be {@code null}.
     * @param routesXML
     *            Route definition as XML resource. Can be {@code null}.
     * @return The service provider implemented with the given Camel routes.
     * @throws URISyntaxException
     */
    public static ProvidesServiceConfiguration createHelloServiceProvider(final URL wsdl,
            final @Nullable Class<?> routesClazz, final @Nullable URL routesXML) throws URISyntaxException {

        final ProvidesServiceConfiguration provides = new ProvidesServiceConfiguration(HELLO_INTERFACE, HELLO_SERVICE,
                HELLO_ENDPOINT, wsdl);

        if (routesClazz != null) {
            provides.setServicesSectionParameter(EL_SERVICES_ROUTE_CLASS, routesClazz.getName());
        }

        if (routesXML != null) {
            provides.setServicesSectionParameter(EL_SERVICES_ROUTE_XML, new File(routesXML.toURI()).getName());
            provides.addResource(routesXML);
        }

        return provides;
    }

    protected static void deployHello(final String suName, final URL wsdl, final Class<?> clazz) throws Exception {
        COMPONENT_UNDER_TEST.deployService(suName, createHelloService(wsdl, clazz, null));
    }

    protected static void deployHello(final String suName, final URL wsdl, final URL routes) throws Exception {
        COMPONENT_UNDER_TEST.deployService(suName, createHelloService(wsdl, null, routes));
    }

    protected static void sendHelloIdentity(final String suName) throws Exception {
        sendHelloIdentity(suName, MessageChecks.none());
    }

    protected static void sendHelloIdentity(final String suName, final MessageChecks serviceChecks)
            throws Exception {
        final String requestContent = "<aaa/>";
        final String responseContent = "<bbb/>";

        sendHello(suName, requestContent, requestContent, responseContent, responseContent, serviceChecks);
    }

    protected static void sendHello(final String suName, @Nullable final String request,
            @Nullable final String expectedRequest, final String response, @Nullable final String expectedResponse,
            final MessageChecks serviceChecks)
            throws Exception {

        MessageChecks reqChecks = isHelloRequest().andThen(serviceChecks);
        if (expectedRequest != null) {
            reqChecks = reqChecks.andThen(MessageChecks.hasXmlContent(expectedRequest));
        }

        MessageChecks respChecks = MessageChecks.noError().andThen(MessageChecks.noFault());
        if (expectedResponse != null) {
            respChecks = respChecks.andThen(MessageChecks.hasXmlContent(expectedResponse));
        }

        // TODO for now we have to disable acknowledgement check (null parameter below) because we don't forward DONE in
        // Camel (see PetalsCamelConsumer)
        COMPONENT.sendAndCheckResponseAndSendStatus(helloRequest(suName, request),
                ServiceProviderImplementation.outMessage(response, null).with(reqChecks), respChecks,
                ExchangeStatus.DONE);
    }

    protected static MessageChecks isHelloRequest() {
        return new MessageChecks() {
            @Override
            public void checks(@Nullable Message request) throws Exception {
                assert request != null;
                final MessageExchange exchange = request.getMessageExchange();
                assertEquals(HELLO_INTERFACE, exchange.getInterfaceName());
                assertEquals(HELLO_SERVICE, exchange.getService());
                assertEquals(HELLO_OPERATION, exchange.getOperation());
                assertEquals(EXTERNAL_ENDPOINT_NAME, exchange.getEndpoint().getEndpointName());
            }
        };
    }

    protected static RequestMessage helloRequest(final String suName, final @Nullable String requestContent) {
        return new RequestToProviderMessage(COMPONENT_UNDER_TEST, suName, HELLO_OPERATION,
                AbsItfOperation.MEPPatternConstants.IN_OUT.value(), requestContent);
    }
}
