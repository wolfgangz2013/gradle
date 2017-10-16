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

package org.gradle.api.internal.artifacts.repositories.resolver

import org.gradle.api.artifacts.ComponentDependencyMetadata
import org.gradle.api.artifacts.ComponentDependencyMetadataDetails
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleVersionSelector
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.internal.artifacts.DefaultModuleVersionSelector
import org.gradle.api.internal.attributes.ImmutableAttributes
import org.gradle.api.internal.notations.DependencyMetadataNotationParser
import org.gradle.internal.component.external.model.DefaultModuleComponentSelector
import org.gradle.internal.component.model.DependencyMetadata
import org.gradle.internal.component.model.Exclude
import org.gradle.internal.component.model.IvyArtifactName
import org.gradle.internal.component.model.LocalComponentDependencyMetadata
import org.gradle.internal.reflect.DirectInstantiator
import spock.lang.Specification

class ComponentDependenciesMetadataDetailsAdapterTest extends Specification {
    List<DependencyMetadata> dependenciesMetadata = []
    def adapter = new ComponentDependenciesMetadataDetailsAdapter(dependenciesMetadata, DependencyMetadataNotationParser.parser(DirectInstantiator.INSTANCE))

    def "add via string id is propagate to the underlying dependency list"() {
        when:
        adapter.add "org.gradle.test:module1:1.0"

        then:
        dependenciesMetadata.size() == 1
        dependenciesMetadata[0].requested.group == "org.gradle.test"
        dependenciesMetadata[0].requested.name == "module1"
        dependenciesMetadata[0].requested.version == "1.0"
    }

    def "add via map id propagate to the underlying dependency list"() {
        when:
        adapter.add group: "org.gradle.test", name: "module1", version: "1.0"

        then:
        dependenciesMetadata.size() == 1
        dependenciesMetadata[0].requested.group == "org.gradle.test"
        dependenciesMetadata[0].requested.name == "module1"
        dependenciesMetadata[0].requested.version == "1.0"
    }

    def "add via string id with action is propagate to the underlying dependency list"() {
        when:
        adapter.add("org.gradle.test:module1:1.0") {
            it.changing = true
            it.force = true
            it.transitive = true
        }

        then:
        dependenciesMetadata.size() == 1
        dependenciesMetadata[0].requested.group == "org.gradle.test"
        dependenciesMetadata[0].requested.name == "module1"
        dependenciesMetadata[0].requested.version == "1.0"
        dependenciesMetadata[0].changing
        dependenciesMetadata[0].force
        dependenciesMetadata[0].transitive
    }

    def "add via map id with action propagate to the underlying dependency list"() {
        when:
        adapter.add(group: "org.gradle.test", name: "module1", version: "1.0") {
            it.changing = true
            it.force = true
            it.transitive = true
        }

        then:
        dependenciesMetadata.size() == 1
        dependenciesMetadata[0].requested.group == "org.gradle.test"
        dependenciesMetadata[0].requested.name == "module1"
        dependenciesMetadata[0].requested.version == "1.0"
        dependenciesMetadata[0].changing
        dependenciesMetadata[0].force
        dependenciesMetadata[0].transitive
    }

    def "remove is propagated to the underlying dependency list"() {
        given:
        fillDependencyList(1)

        when:
        adapter.removeAll { true }

        then:
        dependenciesMetadata == []
    }

    def "adapters for list items are created lazyly"() {
        when:
        fillDependencyList(2)

        then:
        dependenciesMetadata.size() == 2
        adapter.componentDependencyMetadataAdapters.size() == 0

        when:
        ++adapter.iterator()

        then:
        dependenciesMetadata.size() == 2
        adapter.componentDependencyMetadataAdapters.size() == 1

        when:
        adapter.each {}

        then:
        dependenciesMetadata.size() == 2
        adapter.componentDependencyMetadataAdapters.size() == 2
    }

    def "size check is propagated to the underlying dependency list"() {
        when:
        fillDependencyList(3)

        then:
        dependenciesMetadata.size() == 3
        adapter.size() == 3
        adapter.componentDependencyMetadataAdapters.size() == 0
    }

    def "iterator returns immutable view on list items"() {
        given:
        fillDependencyList(1)

        when:
        def dependencyMetadata = ++adapter.iterator()

        then:
        dependencyMetadata instanceof ComponentDependencyMetadata
        !(dependencyMetadata instanceof ComponentDependencyMetadataDetails)
    }

    private fillDependencyList(int size) {
        dependenciesMetadata = []
        (1..size).each {
            ModuleVersionSelector requested = new DefaultModuleVersionSelector("org.gradle.test", "module$size", "1.0")
            ModuleComponentSelector selector = DefaultModuleComponentSelector.newSelector(requested)
            dependenciesMetadata += [ new LocalComponentDependencyMetadata(selector, requested,
                Dependency.DEFAULT_CONFIGURATION, ImmutableAttributes.EMPTY, Dependency.DEFAULT_CONFIGURATION,
                Collections.<IvyArtifactName>emptySet(), Collections.<Exclude>emptyList(),
                false, false , false)
            ]
        }
        adapter = new ComponentDependenciesMetadataDetailsAdapter(dependenciesMetadata, DependencyMetadataNotationParser.parser(DirectInstantiator.INSTANCE))
    }
}
