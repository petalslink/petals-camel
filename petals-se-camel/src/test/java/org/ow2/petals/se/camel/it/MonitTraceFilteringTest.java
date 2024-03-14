/**
 * Copyright (c) 2019-2024 Linagora
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
package org.ow2.petals.se.camel.it;

import java.net.URISyntaxException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfOperation.MEPPatternConstants;
import org.ow2.petals.ObjectFactory;
import org.ow2.petals.SayHello;
import org.ow2.petals.SayHelloResponse;
import org.ow2.petals.component.framework.jbidescriptor.generated.MEPType;
import org.ow2.petals.component.framework.junit.impl.ConsumesServiceConfiguration;
import org.ow2.petals.component.framework.junit.impl.ProvidesServiceConfiguration;
import org.ow2.petals.component.framework.junit.monitoring.business.filtering.AbstractMonitTraceFilteringTestForSimpleOrchestration;
import org.ow2.petals.component.framework.junit.monitoring.business.filtering.exception.ServiceProviderCfgCreationError;
import org.ow2.petals.se.camel.AbstractComponentTest;
import org.ow2.petals.se.camel.mocks.TestRoutesOK;

import com.ebmwebsourcing.easycommons.lang.UncheckedException;

/**
 * Unit tests about MONIT trace filtering.
 * 
 * @author Christophe DENEUX - Linagora
 * 
 */
public class MonitTraceFilteringTest extends AbstractMonitTraceFilteringTestForSimpleOrchestration {

    private static Marshaller MARSHALLER;

    static {
        try {
            final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class);
            MARSHALLER = context.createMarshaller();
            MARSHALLER.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        } catch (final JAXBException e) {
            throw new UncheckedException(e);
        }
    }

    @Override
    protected String getConsumedServiceEndpoint() {
        return AbstractComponentTest.EXTERNAL_ENDPOINT_NAME;
    }

    @Override
    protected QName getConsumedServiceName() {
        return AbstractComponentTest.HELLO_SERVICE;
    }

    @Override
    protected QName getConsumedServiceInterface() {
        return AbstractComponentTest.HELLO_INTERFACE;
    }

    @Override
    protected QName getConsumedServiceOperation(final MEPPatternConstants mep) {
        return AbstractComponentTest.HELLO_OPERATION;
    }

    @Override
    protected QName getInvokedServiceProviderOperation(final MEPPatternConstants mep) {

        if (mep == MEPPatternConstants.IN_OUT) {
            return AbstractComponentTest.HELLO_OPERATION;
        } else if (mep == MEPPatternConstants.ROBUST_IN_ONLY) {
            return AbstractComponentTest.HELLO_WITHOUT_ECHO_ROBUST_OPERATION;
        } else {
            return AbstractComponentTest.HELLO_WITHOUT_ECHO_OPERATION;
        }
    }

    @Override
    protected Marshaller getMarshaller() {
        return MARSHALLER;
    }

    @Override
    protected MEPPatternConstants[] getMepsSupported() {
        return new MEPPatternConstants[] { MEPPatternConstants.IN_ONLY, MEPPatternConstants.IN_OUT,
                MEPPatternConstants.ROBUST_IN_ONLY };
    }

    @Override
    protected Object createRequestPayloadToProvider(final MEPPatternConstants mep) {
        return new ObjectFactory().createSayHello(new SayHello());
    }

    @Override
    protected Object createResponsePayloadToProvider(final MEPPatternConstants mep, final boolean useAsFault) {
        return new ObjectFactory().createSayHelloResponse(new SayHelloResponse());
    }

    @Override
    protected ProvidesServiceConfiguration createServiceProvider(final int ruleIdx)
            throws ServiceProviderCfgCreationError {

        try {
            return AbstractComponentTest.createHelloServiceProvider(AbstractComponentTest.WSDL11, TestRoutesOK.class,
                    null);
        } catch (final URISyntaxException e) {
            throw new ServiceProviderCfgCreationError(e);
        }
    }

    @Override
    protected ConsumesServiceConfiguration[] createServiceConsumers(final int ruleIdx, final MEPType mep) {
        final ConsumesServiceConfiguration consumesCfg = AbstractComponentTest.createHelloConsumes();
        consumesCfg.setMEP(mep);
        return new ConsumesServiceConfiguration[] { consumesCfg };
    }

}
