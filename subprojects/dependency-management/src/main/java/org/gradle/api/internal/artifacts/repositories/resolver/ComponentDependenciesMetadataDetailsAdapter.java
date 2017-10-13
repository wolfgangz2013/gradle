/*
 * Copyright 2017 the original author or authors.
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

import com.google.common.collect.Maps;
import org.gradle.api.Action;
import org.gradle.api.artifacts.ComponentDependenciesMetadataDetails;
import org.gradle.api.artifacts.ComponentDependencyMetadata;
import org.gradle.api.artifacts.ComponentDependencyMetadataDetails;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.internal.artifacts.DefaultModuleVersionSelector;
import org.gradle.api.internal.attributes.ImmutableAttributes;
import org.gradle.internal.component.external.model.DefaultModuleComponentSelector;
import org.gradle.internal.component.model.DependencyMetadata;
import org.gradle.internal.component.model.Exclude;
import org.gradle.internal.component.model.IvyArtifactName;
import org.gradle.internal.component.model.LocalComponentDependencyMetadata;
import org.gradle.internal.typeconversion.NotationParser;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ComponentDependenciesMetadataDetailsAdapter extends AbstractList<ComponentDependencyMetadata> implements ComponentDependenciesMetadataDetails {
    private final List<DependencyMetadata> dependenciesMetadata;
    private final Map<Integer, ComponentDependencyMetadata> componentDependencyMetadataAdapters;
    private final NotationParser<Object, ComponentDependencyMetadataDetails> dependencyMetadataNotationParser;

    public ComponentDependenciesMetadataDetailsAdapter(List<DependencyMetadata> dependenciesMetadata, NotationParser<Object, ComponentDependencyMetadataDetails> dependencyMetadataNotationParser) {
        this.dependenciesMetadata = dependenciesMetadata;
        this.componentDependencyMetadataAdapters = Maps.newHashMap();
        this.dependencyMetadataNotationParser = dependencyMetadataNotationParser;
    }

    @Override
    public ComponentDependencyMetadata get(int index) {
        if (!componentDependencyMetadataAdapters.containsKey(index)) {
            componentDependencyMetadataAdapters.put(index, new ComponentDependencyMetadataAdapter(dependenciesMetadata.get(index)));
        }
        return componentDependencyMetadataAdapters.get(index);
    }

    @Override
    public int size() {
        return dependenciesMetadata.size();
    }

    @Override
    public ComponentDependencyMetadata remove(int index) {
        ComponentDependencyMetadata componentDependencyMetadata = get(index);
        dependenciesMetadata.remove(index);
        componentDependencyMetadataAdapters.remove(index);
        return componentDependencyMetadata;
    }

    @Override
    public void add(Object dependencyNotation, Action<ComponentDependencyMetadataDetails> configureAction) {
        ComponentDependencyMetadataDetails componentDependencyMetadataDetails = dependencyMetadataNotationParser.parseNotation(dependencyNotation);
        configureAction.execute(componentDependencyMetadataDetails);
        dependenciesMetadata.add(toDependencyMetadata(componentDependencyMetadataDetails));
    }

    private DependencyMetadata toDependencyMetadata(ComponentDependencyMetadataDetails details) {
        ModuleVersionSelector requested = new DefaultModuleVersionSelector(details.getGroup(), details.getName(), details.getVersion());
        ModuleComponentSelector selector = DefaultModuleComponentSelector.newSelector(requested);

        return new LocalComponentDependencyMetadata(selector, requested,
            Dependency.DEFAULT_CONFIGURATION, ImmutableAttributes.EMPTY, Dependency.DEFAULT_CONFIGURATION,
            Collections.<IvyArtifactName>emptySet(), Collections.<Exclude>emptyList(),
            details.isForce(), details.isChanging(), details.isTransitive());
    }
}
