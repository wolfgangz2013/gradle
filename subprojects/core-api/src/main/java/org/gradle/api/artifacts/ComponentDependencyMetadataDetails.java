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

package org.gradle.api.artifacts;

import org.gradle.api.Incubating;

/**
 * @since 4.4
 */
@Incubating
public interface ComponentDependencyMetadataDetails extends ComponentDependencyMetadata {

    /**
     * Sets whether or not Gradle should always check for a change in the remote repository. If set to true, Gradle will
     * check the remote repository even if a dependency with the same version is already in the local cache. Defaults to
     * false.
     *
     * @param changing Whether or not Gradle should always check for a change in the remote repository
     * @return this
     */
    ComponentDependencyMetadataDetails setChanging(boolean changing);

    /**
     * Sets whether this dependency should be resolved including or excluding its transitive dependencies. The artifacts
     * belonging to this dependency might themselves have dependencies on other artifacts. The latter are called
     * transitive dependencies.
     *
     * @param transitive Whether transitive dependencies should be resolved.
     * @return this
     */
    ComponentDependencyMetadataDetails setTransitive(boolean transitive);

    /**
     * Sets whether or not the version of this dependency should be enforced in the case of version conflicts.
     *
     * @param force Whether to force this version or not.
     * @return this
     */
    ComponentDependencyMetadataDetails setForce(boolean force);
}
