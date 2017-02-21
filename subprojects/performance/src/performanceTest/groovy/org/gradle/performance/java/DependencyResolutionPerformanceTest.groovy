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

package org.gradle.performance.java

import org.gradle.performance.AbstractCrossVersionPerformanceTest
import spock.lang.Unroll

class DependencyResolutionPerformanceTest extends AbstractCrossVersionPerformanceTest {
    @Unroll("Resolves dependencies from #repoType repository - #testProject (daemon)")
    def "full build Java build"() {
        given:
        runner.testId = "resolves dependencies from $repoType repository $testProject (daemon)"
        runner.testProject = testProject
        runner.tasksToRun = ['resolveDependencies']
        runner.gradleOpts = ["-Xms256m", "-Xmx256m"]

        when:
        def result = runner.run()

        then:
        result.assertCurrentVersionHasNotRegressed()

        where:
        testProject              | repoType
        "lotDependencies"        | 'local'
        "lotProjectDependencies" | 'local'
    }

    def "exclude rule merging"() {
        given:
        runner.testId = "merges exclude rules (daemon)"
        runner.testProject = "excludeRuleMergingBuild"
        runner.tasksToRun = ['resolveDependencies']
        runner.gradleOpts = ["-Xms1g", "-Xmx1g"]
        runner.targetVersions = ["3.5-20170221000043+0000"]
        when:
        def result = runner.run()

        then:
        result.assertCurrentVersionHasNotRegressed()
    }
}