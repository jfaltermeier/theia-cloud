/********************************************************************************
 * Copyright (C) 2022 EclipseSource, Lockular, Ericsson, STMicroelectronics and 
 * others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 ********************************************************************************/
package org.eclipse.theia.cloud.operator.handler;

import static org.eclipse.theia.cloud.operator.util.LogMessageUtil.formatLogMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.operator.resource.TemplateSpecResource;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Service;

public final class TheiaCloudServiceUtil {

    private static final Logger LOGGER = LogManager.getLogger(TheiaCloudServiceUtil.class);

    public static final String SERVICE_NAME = "-service-";

    public static final String PLACEHOLDER_SERVICENAME = "placeholder-servicename";

    private TheiaCloudServiceUtil() {
    }

    public static String getServiceName(TemplateSpecResource template, int instance) {
	return getServiceNamePrefix(template) + instance;
    }

    private static String getServiceNamePrefix(TemplateSpecResource template) {
	return template.getSpec().getName() + SERVICE_NAME;
    }

    public static Integer getId(String correlationId, TemplateSpecResource template, Service service) {
	int namePrefixLength = getServiceNamePrefix(template).length();
	String name = service.getMetadata().getName();
	String instance = name.substring(namePrefixLength);
	try {
	    return Integer.valueOf(instance);
	} catch (NumberFormatException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while getting integer value of " + instance), e);
	}
	return null;
    }

    public static Set<Integer> computeIdsOfMissingServices(TemplateSpecResource template, String correlationId,
	    int instances, List<Service> existingItems) {
	return TheiaCloudHandlerUtil.computeIdsOfMissingItems(instances, existingItems,
		service -> getId(correlationId, template, service));
    }

    public static Map<String, String> getServiceReplacements(String namespace, TemplateSpecResource template,
	    int instance) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(PLACEHOLDER_SERVICENAME, getServiceName(template, instance));
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_APP,
		TheiaCloudHandlerUtil.getAppSelector(template, instance));
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_PORT, String.valueOf(template.getSpec().getPort()));
	return replacements;
    }

    public static Optional<Service> getServiceOwnedByWorkspace(String workspaceResourceName,
	    String workspaceResourceUID, List<Service> existingServices) {
	Optional<Service> alreadyReservedService = existingServices.stream()//
		.filter(service -> {
		    if (isUnusedService(service)) {
			return false;
		    }
		    for (OwnerReference ownerReference : service.getMetadata().getOwnerReferences()) {
			if (workspaceResourceName.equals(ownerReference.getName())
				&& workspaceResourceUID.equals(ownerReference.getUid())) {
			    return true;
			}
		    }
		    return false;
		})//
		.findAny();
	return alreadyReservedService;
    }

    public static boolean isUnusedService(Service service) {
	return service.getMetadata().getOwnerReferences().size() == 1;
    }

    public static Optional<Service> getUnusedService(List<Service> existingServices) {
	Optional<Service> serviceToUse = existingServices.stream()//
		.filter(TheiaCloudServiceUtil::isUnusedService)//
		.findAny();
	return serviceToUse;
    }

}
