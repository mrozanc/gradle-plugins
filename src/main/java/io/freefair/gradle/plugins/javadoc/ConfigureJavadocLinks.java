package io.freefair.gradle.plugins.javadoc;

import lombok.Getter;
import lombok.Setter;
import org.gradle.api.DefaultTask;
import org.gradle.api.JavaVersion;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.MinimalJavadocOptions;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;

/**
 * @author Lars Grefer
 */
@Getter
@Setter
public class ConfigureJavadocLinks extends DefaultTask {

    private Javadoc javadoc;

    @InputFiles
    private Configuration configuration;

    @Input
    private JavaVersion javaVersion;

    @TaskAction
    public void configureJavadocLinks() {

        switch (getJavaVersion()) {
            case VERSION_1_5:
                addLink("https://docs.oracle.com/javase/5/docs/api/");
                break;
            case VERSION_1_6:
                addLink("https://docs.oracle.com/javase/6/docs/api/");
                break;
            case VERSION_1_7:
                addLink("https://docs.oracle.com/javase/7/docs/api/");
                break;
            case VERSION_1_8:
                addLink("https://docs.oracle.com/javase/8/docs/api/");
                break;
            case VERSION_1_9:
                addLink("https://docs.oracle.com/javase/9/docs/api/");
                break;
            default:
                getLogger().warn("Unknown java version {}", javaVersion);
        }

        boolean usedJavadocIo = false;

        for (ResolvedArtifact resolvedArtifact : getConfiguration().getResolvedConfiguration().getResolvedArtifacts()) {
            ComponentIdentifier componentIdentifier = resolvedArtifact.getId().getComponentIdentifier();

            if (componentIdentifier instanceof ModuleComponentIdentifier) {
                ModuleComponentIdentifier moduleComponentIdentifier = (ModuleComponentIdentifier) componentIdentifier;
                String group = moduleComponentIdentifier.getGroup();
                String artifact = moduleComponentIdentifier.getModule();
                String version = moduleComponentIdentifier.getVersion();

                String wellKnownLink = findWellKnownLink(group, artifact, version);
                if (wellKnownLink != null) {
                    getLogger().info("Using well known link '{}' for '{}:{}:{}'", wellKnownLink, group, artifact, version);
                    addLink(wellKnownLink);
                    break;
                } else {
                    getLogger().info("Using javadoc.io link for '{}:{}:{}'", group, artifact, version);
                    String javadocIoLink = String.format("https://static.javadoc.io/%s/%s/%s/", group, artifact, version);
                    addLink(javadocIoLink);
                    usedJavadocIo = true;
                }
            }
        }

        if(usedJavadocIo) {
            javadoc.getOptions().jFlags("-Dhttp.agent=" + System.currentTimeMillis());
        }
    }

    private String findWellKnownLink(String group, String artifact, String version) {
        if (group.equals("org.springframework") && artifact.startsWith("spring-")) {
            return "https://docs.spring.io/spring/docs/" + version + "/javadoc-api/";
        } else if (group.equals("org.springframework.boot")) {
            return "https://docs.spring.io/spring-boot/docs/" + version + "/api/";
        }
        return null;
    }

    private void addLink(String baseUrl) {
        MinimalJavadocOptions options = javadoc.getOptions();
        if (options instanceof StandardJavadocDocletOptions) {
            StandardJavadocDocletOptions docletOptions = (StandardJavadocDocletOptions) options;
            if (!docletOptions.getLinks().contains(baseUrl)) {
                getLogger().debug("Adding '{}' to {}", baseUrl, javadoc);
                docletOptions.links(baseUrl);
                setDidWork(true);
            } else {
                getLogger().info("Not adding '{}' to {} because it's already present", baseUrl, javadoc);
            }
        }
    }
}
