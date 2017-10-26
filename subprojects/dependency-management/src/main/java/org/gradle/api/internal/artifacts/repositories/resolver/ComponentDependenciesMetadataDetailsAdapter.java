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
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.internal.artifacts.DefaultModuleVersionSelector;
import org.gradle.internal.component.model.DependencyMetadata;
import org.gradle.internal.component.model.GradleDependencyMetadata;
import org.gradle.internal.typeconversion.NotationParser;

import javax.annotation.Nullable;
import java.util.AbstractList;
import java.util.List;
import java.util.Map;

public class ComponentDependenciesMetadataDetailsAdapter extends AbstractList<ComponentDependencyMetadata> implements ComponentDependenciesMetadataDetails {
    private final List<DependencyMetadata> dependenciesMetadata;
    private final Map<Integer, ComponentDependencyMetadataDetails> componentDependencyMetadataAdapters;
    private final NotationParser<Object, ComponentDependencyMetadataDetails> dependencyMetadataNotationParser;

    public ComponentDependenciesMetadataDetailsAdapter(List<DependencyMetadata> dependenciesMetadata, NotationParser<Object, ComponentDependencyMetadataDetails> dependencyMetadataNotationParser) {
        this.dependenciesMetadata = dependenciesMetadata;
        this.componentDependencyMetadataAdapters = Maps.newHashMap();
        this.dependencyMetadataNotationParser = dependencyMetadataNotationParser;
    }

    @Override
    public ComponentDependencyMetadataDetails get(int index) {
        if (!componentDependencyMetadataAdapters.containsKey(index)) {
            componentDependencyMetadataAdapters.put(index, new IndexedComponentDependencyMetadataDetails(index));
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
        componentDependencyMetadataAdapters.clear();
        return componentDependencyMetadata;
    }

    @Override
    public void add(String dependencyNotation) {
        doAdd(dependencyNotation, null);
    }

    @Override
    public void add(Map<String, String> dependencyNotation) {
        doAdd(dependencyNotation, null);
    }

    @Override
    public void add(String dependencyNotation, Action<ComponentDependencyMetadataDetails> configureAction) {
        doAdd(dependencyNotation, configureAction);
    }

    @Override
    public void add(Map<String, String> dependencyNotation, Action<ComponentDependencyMetadataDetails> configureAction) {
        doAdd(dependencyNotation, configureAction);
    }

    private void doAdd(Object dependencyNotation, @Nullable Action<ComponentDependencyMetadataDetails> configureAction) {
        ComponentDependencyMetadataDetails componentDependencyMetadataDetails = dependencyMetadataNotationParser.parseNotation(dependencyNotation);
        if (configureAction != null) {
            configureAction.execute(componentDependencyMetadataDetails);
        }
        maybeRemoveExistingDependency(componentDependencyMetadataDetails);
        dependenciesMetadata.add(toDependencyMetadata(componentDependencyMetadataDetails));
    }

    private void maybeRemoveExistingDependency(ComponentDependencyMetadataDetails newDependency) {
        for (int index = 0; index < dependenciesMetadata.size(); index++) {
            DependencyMetadata dependency = dependenciesMetadata.get(index);
            if (newDependency.getName().equals(dependency.getRequested().getName()) && newDependency.getGroup().equals(dependency.getRequested().getGroup())) {
                remove(index);
                return;
            }
        }
    }

    private DependencyMetadata toDependencyMetadata(ComponentDependencyMetadataDetails details) {
        ModuleVersionSelector requested = new DefaultModuleVersionSelector(details.getGroup(), details.getName(), details.getVersion());
        return new GradleDependencyMetadata(requested);
    }

    private class IndexedComponentDependencyMetadataDetails implements ComponentDependencyMetadataDetails {
        private final int index;

        private IndexedComponentDependencyMetadataDetails(int index) {
            this.index = index;
        }

        private DependencyMetadata get() {
            return dependenciesMetadata.get(index);
        }

        @Override
        public ComponentDependencyMetadataDetails setVersion(String version) {
            DependencyMetadata original = get();
            DependencyMetadata newDep = original.withRequestedVersion(version);
            dependenciesMetadata.set(index, newDep);
            return this;
        }

        @Override
        public String getGroup() {
            return get().getRequested().getGroup();
        }

        @Override
        public String getName() {
            return get().getRequested().getName();
        }

        @Override
        public String getVersion() {
            return get().getRequested().getVersion();
        }
    }
}
