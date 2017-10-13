/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.artifacts.repositories.resolver;

import org.gradle.api.artifacts.ComponentDependenciesMetadataDetails;
import org.gradle.api.artifacts.ComponentDependencyMetadataDetails;
import org.gradle.api.artifacts.ComponentMetadataDetails;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.internal.component.external.model.MutableModuleComponentResolveMetadata;
import org.gradle.internal.component.model.DependencyMetadata;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.typeconversion.NotationParser;

import java.util.List;

public class ComponentMetadataDetailsAdapter implements ComponentMetadataDetails {
    private final MutableModuleComponentResolveMetadata metadata;
    private final List<DependencyMetadata> dependenciesMetadata;
    private final Instantiator instantiator;
    private final NotationParser<Object, ComponentDependencyMetadataDetails> dependencyMetadataNotationParser;
    private ComponentDependenciesMetadataDetails componentDependenciesMetadataDetails;

    public ComponentMetadataDetailsAdapter(MutableModuleComponentResolveMetadata metadata, List<DependencyMetadata> dependenciesMetadata, Instantiator instantiator, NotationParser<Object, ComponentDependencyMetadataDetails> dependencyMetadataNotationParser) {
        this.metadata = metadata;
        this.dependenciesMetadata = dependenciesMetadata;
        this.instantiator = instantiator;
        this.dependencyMetadataNotationParser = dependencyMetadataNotationParser;
    }

    @Override
    public ModuleVersionIdentifier getId() {
        return metadata.getId();
    }

    @Override
    public boolean isChanging() {
        return metadata.isChanging();
    }

    public String getStatus() {
        return metadata.getStatus();
    }

    @Override
    public List<String> getStatusScheme() {
        return metadata.getStatusScheme();
    }

    @Override
    public void setChanging(boolean changing) {
        metadata.setChanging(changing);
    }

    @Override
    public void setStatus(String status) {
        metadata.setStatus(status);
    }

    @Override
    public void setStatusScheme(List<String> statusScheme) {
        metadata.setStatusScheme(statusScheme);
    }

    @Override
    public ComponentDependenciesMetadataDetails getDependencies() {
        if (componentDependenciesMetadataDetails == null) {
            componentDependenciesMetadataDetails = instantiator.newInstance(ComponentDependenciesMetadataDetailsAdapter.class,
                dependenciesMetadata, dependencyMetadataNotationParser);
        }
        return componentDependenciesMetadataDetails;
    }
}
