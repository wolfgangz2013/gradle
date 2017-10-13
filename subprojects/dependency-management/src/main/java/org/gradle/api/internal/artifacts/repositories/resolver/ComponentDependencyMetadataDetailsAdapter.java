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

import org.gradle.api.artifacts.ComponentDependencyMetadataDetails;

public class ComponentDependencyMetadataDetailsAdapter implements ComponentDependencyMetadataDetails {
    private final String group;
    private final String name;
    private final String version;

    private boolean changing;
    private boolean force;
    private boolean transitive;

    public ComponentDependencyMetadataDetailsAdapter(String group, String name, String version) {
        this.group = group;
        this.name = name;
        this.version = version;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    @Override
    public boolean isChanging() {
        return this.changing;
    }

    @Override
    public ComponentDependencyMetadataDetails setChanging(boolean changing) {
        this.changing = changing;
        return this;
    }


    @Override
    public boolean isTransitive() {
        return this.transitive;
    }

    @Override
    public ComponentDependencyMetadataDetails setTransitive(boolean transitive) {
        this.transitive = transitive;
        return this;
    }

    @Override
    public boolean isForce() {
        return this.force;
    }

    @Override
    public ComponentDependencyMetadataDetails setForce(boolean force) {
        this.force = force;
        return this;
    }
}
