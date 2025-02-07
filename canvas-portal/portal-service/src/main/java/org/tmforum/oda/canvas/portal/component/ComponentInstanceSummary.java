package org.tmforum.oda.canvas.portal.component;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmforum.oda.canvas.portal.core.exception.BaseAppException;
import org.tmforum.oda.canvas.portal.core.util.JsonPathUtil;
import org.tmforum.oda.canvas.portal.core.util.JsonUtil;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMeta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;


/**
 * ODA Component instance summary
 *
 * @author li.peilong
 * @date 2022/12/01
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentInstanceSummary {
    private String name;
    private String type;
    private String namespace;
    private String release;
    private String domain;
    private String version;
    private String vendor;
    private String status;
    private Date createTime;
    private String description;

    private static final String UNKNOWN_STATUS = "Unknown";

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentInstanceSummary.class);

    public static ComponentInstanceSummary from(GenericKubernetesResource genericKubernetesResource) throws BaseAppException {
        String spec = JsonUtil.object2Json(genericKubernetesResource.getAdditionalProperties().get("spec"));
        String status = JsonUtil.object2Json(genericKubernetesResource.getAdditionalProperties().get("status"));
        String name = genericKubernetesResource.getMetadata().getName();
        String type = JsonPathUtil.findOne(spec, "$.componentMetadata.type");
        if (type == null) {
            type = JsonPathUtil.findOne(spec, "$.componentMetadata.id") + "-" + JsonPathUtil.findOne(spec, "$.componentMetadata.name");
        }
        ComponentType componentType = ComponentType.from(type);
        // FIXME: get vendor?
        String vendor = name.split("-")[0];
        return ComponentInstanceSummary.builder()
                .name(genericKubernetesResource.getMetadata().getName())
                .namespace(genericKubernetesResource.getMetadata().getNamespace())
                .type(componentType.getName())
                .vendor(vendor)
                .domain(componentType.getDomain())
                .release(getReleaseName(genericKubernetesResource.getMetadata()))
                .version(JsonPathUtil.findOne(spec, "$.componentMetadata.version"))
                .description(JsonPathUtil.findOne(spec, "$.componentMetadata.description"))
                .status(getStatus(status))
                .createTime(Date.from(Instant.parse(genericKubernetesResource.getMetadata().getCreationTimestamp())))
                .build();
    }

    private static String getReleaseName(ObjectMeta objectMeta) {
        Map<String, String> annotations = objectMeta.getAnnotations();
        return annotations.get("meta.helm.sh/release-name");
    }

    private static String getStatus(String status) {
        if (status == null) {
            return UNKNOWN_STATUS;
        }
        try {
            return JsonPathUtil.findOne(status, "$.summary/status.deployment_status");
        }
        catch (BaseAppException e) {
            LOGGER.debug("find $.summary/status.deployment_status node failed", e);
            return UNKNOWN_STATUS;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }
}
