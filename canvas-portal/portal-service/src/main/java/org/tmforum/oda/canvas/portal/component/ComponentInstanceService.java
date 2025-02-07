package org.tmforum.oda.canvas.portal.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.tmforum.oda.canvas.portal.core.exception.BaseAppException;
import org.tmforum.oda.canvas.portal.core.exception.ExceptionPublisher;
import org.tmforum.oda.canvas.portal.core.util.JsonPathUtil;
import org.tmforum.oda.canvas.portal.core.util.JsonUtil;
import org.tmforum.oda.canvas.portal.infrastructure.CanvasErrorCode;

import com.google.common.collect.Lists;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

/**
 * Provides component resources API
 *
 * @author li.peilong
 * @date 2022/11/30
 * @see "https://rohaan.medium.com/handling-kubernetes-custom-resources-in-java-using-fabric8-kubernetes-client-249c4fe01d27"
 * @see "https://github.com/tmforum-oda/oda-canvas-charts/blob/master/canvas/charts/crds/templates/oda-component-crd.yaml"
 * @see "https://github.com/tmforum-oda/oda-ca-docs/blob/master/ODAComponentDesignGuidelines.md"
 */
@Service
public class ComponentInstanceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentInstanceService.class);
    private final KubernetesClient kubeClient;

    public ComponentInstanceService(KubernetesClient kubeClient) {
        this.kubeClient = kubeClient;
    }

    private final Map<Integer, CustomResourceDefinitionContext> customResourceDefinitionContexts = new ConcurrentHashMap<>();
    public static final String[] SUPPORTED_VERSIONS = new String[]{"v1", "v1beta4", "v1beta3", "v1beta1", "v1alpha4", "v1alpha3", "v1alpha2", "v1alpha1"};

    /**
     * Get a list of ODA Component instances
     *
     * @param namespace namespace
     * @param keyword   Filter by keyword (name or type)
     */
    public List<GenericKubernetesResource> listOdaComponentInstances(String namespace, String keyword) throws BaseAppException {
        try {
            List<GenericKubernetesResource> components = kubeClient.genericKubernetesResources(getCustomResourceDefinitionContext(namespace)).inNamespace(namespace).list().getItems();
            if (StringUtils.isBlank(keyword)) {
                return components;
            }
            return components.stream().filter(component -> {
                if (StringUtils.containsIgnoreCase(component.getMetadata().getName(), keyword)) {
                    return true;
                }
                try {
                    ComponentType type = ComponentType.from(JsonPathUtil.findOne(JsonUtil.object2Json(component), "$.spec.type"));
                    return StringUtils.containsIgnoreCase(type.getName(), keyword);
                }
                catch (Exception e) {
                    LOGGER.warn("Failed to find $.spec.type node in component [{}]", component.getMetadata().getName(), e);
                    return false;
                }
            }).collect(Collectors.toList());
        }
        catch (Exception e) {
            LOGGER.error("Failed to list ODA component instances in namespace[{}], error: ", namespace, e);
            ExceptionPublisher.publish(e, CanvasErrorCode.ODA_LIST_COMPONENT_INSTANCE_FAILED, namespace);
        }
        return Lists.newArrayList();
    }

    /**
     * List events for a specific ODA component instance
     *
     * @param namespace namespace
     * @param component Component name, if not specified, query events for all components
     * @return events with the component
     * @throws BaseAppException exception when list failed
     */
    public List<Event> listOdaComponentInstanceEvents(String namespace, String component) throws BaseAppException {
        try {
            Map<String, String> fields = new HashMap<>();
            fields.put("involvedObject.kind", "component");
            if (StringUtils.isNoneBlank(component)) {
                fields.put("involvedObject.name", component);
            }
            return kubeClient.v1().events().inNamespace(namespace).withFields(fields).list().getItems();
        }
        catch (Exception e) {
            LOGGER.warn("Failed to list component instance events in namespace[{}], error: ", namespace, e);
            ExceptionPublisher.publish(e, CanvasErrorCode.ODA_LIST_COMPONENT_INSTANCE_EVENTS_FAILED, namespace);
        }
        return Lists.newArrayList();
    }

    /**
     * List summaries of ODA component instances
     *
     * @param namespace namespace
     * @param keyword   keyword
     * @return component instance summary list
     * @throws BaseAppException
     */
    public List<ComponentInstanceSummary> listOdaComponentInstancesSummary(String namespace, String keyword) throws BaseAppException {
        List<GenericKubernetesResource> components = listOdaComponentInstances(namespace, "");
        List<ComponentInstanceSummary> result = new ArrayList<>();
        for (GenericKubernetesResource component : components) {
            result.add(ComponentInstanceSummary.from(component));
        }

        return result.stream()
                .filter(componentInstanceSummary -> {
                    return StringUtils.isBlank(keyword)
                            || StringUtils.containsIgnoreCase(componentInstanceSummary.getName(), keyword)
                            || StringUtils.contains(componentInstanceSummary.getType(), keyword)
                            || StringUtils.containsIgnoreCase(componentInstanceSummary.getVendor(), keyword)
                            || StringUtils.containsIgnoreCase(componentInstanceSummary.getDomain(), keyword)
                            || StringUtils.containsIgnoreCase(componentInstanceSummary.getStatus(), keyword);
                }).collect(Collectors.toList());
    }

    /**
     * Get Kubernetes resources associated with a component
     *
     * @param namespace namespace
     * @param name      name
     * @return
     * @throws BaseAppException
     * @see "https://github.com/tmforum-oda/oda-ca-docs/blob/master/ODAComponentDesignGuidelines.md#step-2-add-labels-to-all-the-standard-kubernetes-resources"
     */
    public List<HasMetadata> listOdaComponentInstanceResources(String namespace, String name) throws BaseAppException {
        GenericKubernetesResource component = getOdaComponentInstance(namespace, name);
        String componentData = JsonUtil.object2Json(component);
        String selectorLabelValue = getComponentName(componentData);
        // Get the types of K8S resources related to the component, TODO: Is there a better way?
        List<String> kinds = getComponentResourceKinds(componentData);
        List<HasMetadata> resources = new ArrayList<>();
        // Include services, deployments, persistentvolumeclaims, jobs, cronjobs, statefulsets, configmap, secret, serviceaccount, role, rolebinding as children of the component
        // These are resources that we support in a component. There are resources that we don't support
        // Reference: https://github.com/tmforum-oda/oda-ca/blob/master/controllers/componentOperator/componentOperator.py
        String selectorLabelKey = "oda.tmforum.org/componentName";
        if (kinds.contains("Service")) {
            resources.addAll(kubeClient.services().inNamespace(namespace).withLabel(selectorLabelKey, selectorLabelValue).list().getItems());
        }
        if (kinds.contains("Deployment")) {
            resources.addAll(kubeClient.apps().deployments().inNamespace(namespace).withLabel(selectorLabelKey, selectorLabelValue).list().getItems());
        }
        if (kinds.contains("StatefulSet")) {
            resources.addAll(kubeClient.apps().statefulSets().inNamespace(namespace).withLabel(selectorLabelKey, selectorLabelValue).list().getItems());
        }
        if (kinds.contains("DaemonSet")) {
            resources.addAll(kubeClient.apps().daemonSets().inNamespace(namespace).withLabel(selectorLabelKey, selectorLabelValue).list().getItems());
        }
        if (kinds.contains("ConfigMap")) {
            resources.addAll(kubeClient.configMaps().inNamespace(namespace).withLabel(selectorLabelKey, selectorLabelValue).list().getItems());
        }
        if (kinds.contains("Secret")) {
            resources.addAll(kubeClient.secrets().inNamespace(namespace).withLabel(selectorLabelKey, selectorLabelValue).list().getItems());
        }
        if (kinds.contains("ServiceAccount")) {
            resources.addAll(kubeClient.serviceAccounts().inNamespace(namespace).withLabel(selectorLabelKey, selectorLabelValue).list().getItems());
        }
        if (kinds.contains("PersistentVolumeClaim")) {
            resources.addAll(kubeClient.persistentVolumeClaims().inNamespace(namespace).withLabel(selectorLabelKey, selectorLabelValue).list().getItems());
        }
        if (kinds.contains("CronJob")) {
            resources.addAll(kubeClient.batch().v1().cronjobs().inNamespace(namespace).withLabel(selectorLabelKey, selectorLabelValue).list().getItems());
        }
        if (kinds.contains("Job")) {
            resources.addAll(kubeClient.batch().v1().jobs().inNamespace(namespace).withLabel(selectorLabelKey, selectorLabelValue).list().getItems());
        }
        if (kinds.contains("Role")) {
            resources.addAll(kubeClient.rbac().roles().inNamespace(namespace).withLabel(selectorLabelKey, selectorLabelValue).list().getItems());
        }
        if (kinds.contains("RoleBinding")) {
            resources.addAll(kubeClient.rbac().roleBindings().inNamespace(namespace).withLabel(selectorLabelKey, selectorLabelValue).list().getItems());
        }
        return resources;
    }

    /**
     * Get the name of the component
     *
     * @param componentData componentData
     * @return
     * @throws BaseAppException
     */
    private String getComponentName(String componentData) throws BaseAppException {
        String jsonPath = "$['metadata']['labels']['oda.tmforum.org/componentName']";
        try {
            return JsonPathUtil.findOne(componentData, jsonPath);
        }
        catch (Exception e) {
            jsonPath = "$['spec']['selector']['matchLabels']['oda.tmforum.org/componentName']";
            return JsonPathUtil.findOne(componentData, jsonPath);
        }
    }

    /**
     * Get the types of resources included in the component
     *
     * @param componentData componentData
     * @return
     * @throws BaseAppException
     */
    private List<String> getComponentResourceKinds(String componentData) {
        try {
            return JsonPathUtil.findList(componentData, "$['spec']['componentKinds'][*]['kind']", String.class);
        }
        catch (Exception e) {
            LOGGER.warn("Failed to get resource kinds for component [{}]", componentData, e);
            // Default to include all types of resources
            return Arrays.asList("Service", "Deployment", "RoleBinding", "Role", "Job", "CronJob", "PersistentVolumeClaim", "ServiceAccount", "Secret", "ConfigMap", "DaemonSet", "StatefulSet");
        }
    }

    /**
     * Get a specific ODA Component instance
     *
     * @param namespace namespace
     * @param name      Component name
     * @return
     */
    public GenericKubernetesResource getOdaComponentInstance(String namespace, String name) throws BaseAppException {
        GenericKubernetesResource component = null;
        try {
            component = kubeClient.genericKubernetesResources(getCustomResourceDefinitionContext(namespace)).inNamespace(namespace).withName(name).get();
        }
        catch (Exception e) {
            LOGGER.warn("Failed to get ODA component instance [{}] in namespace [{}], error: ", name, namespace, e);
            ExceptionPublisher.publish(e, CanvasErrorCode.ODA_GET_COMPONENT_INSTANCE_FAILED, name, namespace);
        }

        if (component == null) {
            ExceptionPublisher.publish(CanvasErrorCode.ODA_COMPONENT_INSTANCE_NOT_EXIST, name, namespace);
        }
        return component;
    }

    /**
     * Get the context for the Custom Resource Definition
     *
     * @return crd context
     * @throws BaseAppException
     */
    public CustomResourceDefinitionContext getCustomResourceDefinitionContext(String namespace) throws BaseAppException {
        synchronized (customResourceDefinitionContexts) {
            for (String version : SUPPORTED_VERSIONS) {
                CustomResourceDefinitionContext customResourceDefinitionContext = new CustomResourceDefinitionContext.Builder()
                        .withName("components.oda.tmforum.org")
                        .withGroup("oda.tmforum.org")
                        .withScope("Namespaced")
                        .withVersion(version)
                        .withPlural("components")
                        .withKind("component")
                        .build();
                try {
                    kubeClient.genericKubernetesResources(customResourceDefinitionContext).inNamespace(namespace).list();
                    return customResourceDefinitionContext;
                }
                catch (Exception e) {
                    LOGGER.warn("CRD components.oda.tmforum.org version is not {}, trying another one", version, e);
                }
            }
        }
        ExceptionPublisher.publish(CanvasErrorCode.ODA_UNSUPPORTED);
        return null;
    }
}
