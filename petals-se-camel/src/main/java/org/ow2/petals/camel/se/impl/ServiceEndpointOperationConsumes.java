/**
 * Copyright (c) 2015-2016 Linagora
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
package org.ow2.petals.camel.se.impl;

import java.net.URI;

import javax.jbi.JBIException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.eclipse.jdt.annotation.Nullable;
import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfOperation.MEPPatternConstants;
import org.ow2.petals.camel.PetalsChannel.PetalsConsumesChannel;
import org.ow2.petals.camel.se.PetalsCamelSender;
import org.ow2.petals.camel.se.exceptions.InvalidJBIConfigurationException;
import org.ow2.petals.component.framework.api.Message.MEPConstants;
import org.ow2.petals.component.framework.api.exception.PEtALSCDKException;
import org.ow2.petals.component.framework.api.message.Exchange;
import org.ow2.petals.component.framework.jbidescriptor.generated.Consumes;
import org.ow2.petals.component.framework.jbidescriptor.generated.MEPType;

public class ServiceEndpointOperationConsumes extends AbstractServiceEndpointOperation implements PetalsConsumesChannel {

    private final Consumes consumes;

    public ServiceEndpointOperationConsumes(final PetalsCamelSender sender, final Consumes consumes)
            throws InvalidJBIConfigurationException {
        super(consumes.getInterfaceName(), consumes.getServiceName(), consumes.getEndpointName(),
                consumes.getOperation(), toMEP(consumes), sender);
        this.consumes = consumes;
    }

    @Override
    public ServiceType getType() {
        return ServiceType.CONSUMES;
    }

    @Override
    public Exchange newExchange() throws JBIException {
        final Exchange exchange = sender.createConsumeExchange(consumes);
        assert exchange != null;
        return exchange;
    }

    @Override
    public Exchange newExchange(final MEPPatternConstants mep) throws JBIException {
        final Exchange exchange = sender.createConsumeExchange(consumes, toMEP(mep));
        assert exchange != null;
        return exchange;
    }

    @Override
    public @Nullable ServiceEndpoint resolveEndpoint(final QName serviceName, final String endpointName) {
        return sender.getComponent().getContext().getEndpoint(serviceName, endpointName);
    }

    private static MEPConstants toMEP(final MEPPatternConstants mep) throws PEtALSCDKException {
        switch (mep) {
            case IN_ONLY:
                return MEPConstants.IN_ONLY_PATTERN;
            case IN_OUT:
                return MEPConstants.IN_OUT_PATTERN;
            case IN_OPTIONAL_OUT:
                return MEPConstants.IN_OPTIONAL_OUT_PATTERN;
            case ROBUST_IN_ONLY:
                return MEPConstants.ROBUST_IN_ONLY_PATTERN;
            default:
                // this can't happen because the mep originally comes from the jbi.xml
                throw new PEtALSCDKException("Impossible case");
        }
    }

    private static @Nullable URI toMEP(final Consumes c) throws InvalidJBIConfigurationException {

        final MEPType mep = c.getMep();

        if (mep == null) {
            return null;
        }

        switch (mep) {
            case IN_ONLY:
                return MEPPatternConstants.IN_ONLY.value();
            case IN_OPTIONAL_OUT:
                return MEPPatternConstants.IN_OPTIONAL_OUT.value();
            case IN_OUT:
                return MEPPatternConstants.IN_OUT.value();
            case ROBUST_IN_ONLY:
                return MEPPatternConstants.ROBUST_IN_ONLY.value();
            default:
                throw new InvalidJBIConfigurationException(
                        "Unknown MEP '" + mep + "' set for operation " + c.getOperation() + " of interface "
                                + c.getInterfaceName() + " and service " + c.getServiceName());
        }
    }
}
