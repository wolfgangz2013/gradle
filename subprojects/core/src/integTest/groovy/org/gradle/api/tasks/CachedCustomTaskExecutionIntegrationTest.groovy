/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.api.tasks

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.DirectoryBuildCacheFixture
import org.gradle.test.fixtures.file.TestFile
import spock.lang.Issue
import spock.lang.Unroll

import static org.gradle.api.tasks.LocalStateFixture.defineTaskWithLocalState

class CachedCustomTaskExecutionIntegrationTest extends AbstractIntegrationSpec implements DirectoryBuildCacheFixture {
    def configureCacheForBuildSrc() {
        file("buildSrc/settings.gradle") << localCacheConfiguration()
    }

    def "buildSrc is loaded from cache"() {
        configureCacheForBuildSrc()
        file("buildSrc/src/main/groovy/MyTask.groovy") << """
            import org.gradle.api.*

            class MyTask extends DefaultTask {}
        """
        assert listCacheFiles().size() == 0
        when:
        withBuildCache().succeeds "tasks"
        then:
        skippedTasks.empty
        listCacheFiles().size() == 1 // compileGroovy

        expect:
        file("buildSrc/build").assertIsDir().deleteDir()

        when:
        withBuildCache().succeeds "tasks"
        then:
        output.contains ":buildSrc:compileGroovy FROM-CACHE"
    }

    def "tasks stay cached after buildSrc with custom Groovy task is rebuilt"() {
        configureCacheForBuildSrc()
        file("buildSrc/src/main/groovy/CustomTask.groovy") << customGroovyTask()
        file("input.txt") << "input"
        buildFile << """
            task customTask(type: CustomTask) {
                inputFile = file "input.txt"
                outputFile = file "build/output.txt"
            }
        """
        when:
        withBuildCache().succeeds "customTask"
        then:
        skippedTasks.empty

        when:
        file("buildSrc/build").deleteDir()
        file("buildSrc/.gradle").deleteDir()
        cleanBuildDir()

        withBuildCache().succeeds "customTask"
        then:
        skippedTasks.contains ":customTask"
    }

    def "changing custom Groovy task implementation in buildSrc doesn't invalidate built-in task"() {
        configureCacheForBuildSrc()
        def taskSourceFile = file("buildSrc/src/main/groovy/CustomTask.groovy")
        taskSourceFile << customGroovyTask()
        file("input.txt") << "input"
        buildFile << """
            task customTask(type: CustomTask) {
                inputFile = file "input.txt"
                outputFile = file "build/output.txt"
            }
        """
        when:
        withBuildCache().succeeds "customTask"
        then:
        skippedTasks.empty
        file("build/output.txt").text == "input"

        when:
        taskSourceFile.text = customGroovyTask(" modified")

        cleanBuildDir()
        withBuildCache().succeeds "customTask"
        then:
        nonSkippedTasks.contains ":customTask"
        file("build/output.txt").text == "input modified"
    }

    private static String customGroovyTask(String suffix = "") {
        """
            import org.gradle.api.*
            import org.gradle.api.tasks.*

            @CacheableTask
            class CustomTask extends DefaultTask {
                @InputFile File inputFile
                @OutputFile File outputFile
                @TaskAction void doSomething() {
                    outputFile.text = inputFile.text + "$suffix"
                }
            }
        """
    }

    def "cacheable task with cache disabled doesn't get cached"() {
        configureCacheForBuildSrc()
        file("input.txt") << "data"
        file("buildSrc/src/main/groovy/CustomTask.groovy") << """
            import org.gradle.api.*
            import org.gradle.api.tasks.*

            @CacheableTask
            class CustomTask extends DefaultTask {
                @InputFile File inputFile
                @OutputFile File outputFile
                @TaskAction void doSomething() {
                    outputFile.text = inputFile.text
                }
            }
        """
        buildFile << """
            task customTask(type: CustomTask) {
                inputFile = file("input.txt")
                outputFile = file("\$buildDir/output.txt")
            }
        """

        when:
        withBuildCache().run "customTask"
        then:
        nonSkippedTasks.contains ":customTask"

        when:
        cleanBuildDir()
        withBuildCache().succeeds "customTask"
        then:
        skippedTasks.contains ":customTask"


        when:
        buildFile << """
            customTask.outputs.cacheIf { false }
        """

        withBuildCache().run "customTask"
        cleanBuildDir()

        withBuildCache().succeeds "customTask"

        then:
        nonSkippedTasks.contains ":customTask"
    }

    def "cacheable task with multiple outputs doesn't get cached"() {
        buildFile << """
            task customTask {
                outputs.cacheIf { true }
                outputs.files files("build/output1.txt", "build/output2.txt")
                doLast {
                    file("build").mkdirs()
                    file("build/output1.txt") << "data"
                    file("build/output2.txt") << "data"
                }
            }
        """

        when:
        withBuildCache().succeeds "customTask", "--info"
        then:
        nonSkippedTasks.contains ":customTask"
        output.contains "Caching disabled for task ':customTask': Declares multiple output files for the single output property '\$1' via `@OutputFiles`, `@OutputDirectories` or `TaskOutputs.files()`"
    }

    def "non-cacheable task with cache enabled gets cached"() {
        file("input.txt") << "data"
        buildFile << """
            class NonCacheableTask extends DefaultTask {
                @InputFile inputFile
                @OutputFile outputFile

                @TaskAction copy() {
                    project.mkdir outputFile.parentFile
                    outputFile.text = inputFile.text
                }
            }
            task customTask(type: NonCacheableTask) {
                inputFile = file("input.txt")
                outputFile = file("\$buildDir/output.txt")
                outputs.cacheIf { true }
            }
        """

        when:
        withBuildCache().run "customTask"
        then:
        nonSkippedTasks.contains ":customTask"

        when:
        cleanBuildDir()
        withBuildCache().succeeds "customTask"
        then:
        skippedTasks.contains ":customTask"
    }

    def "ad hoc tasks are not cacheable by default"() {
        given:
        file("input.txt") << "data"
        buildFile << adHocTaskWithInputs()

        expect:
        taskIsNotCached ':adHocTask'
    }

    def "ad hoc tasks are cached when explicitly requested"() {
        given:
        file("input.txt") << "data"
        buildFile << adHocTaskWithInputs()
        buildFile << 'adHocTask { outputs.cacheIf { true } }'

        expect:
        taskIsCached ':adHocTask'
    }

    private static String adHocTaskWithInputs() {
        """
        task adHocTask {
            def outputFile = file("\$buildDir/output.txt")
            inputs.file(file("input.txt"))
            outputs.file(outputFile)
            doLast {
                project.mkdir outputFile.parentFile
                outputFile.text = file("input.txt").text
            }
        }
        """.stripIndent()
    }

    def "optional file output is not stored when there is no output"() {
        configureCacheForBuildSrc()
        file("input.txt") << "data"
        file("buildSrc/src/main/groovy/CustomTask.groovy") << """
            import org.gradle.api.*
            import org.gradle.api.tasks.*

            @CacheableTask
            class CustomTask extends DefaultTask {
                @InputFile File inputFile
                @OutputFile File outputFile
                @Optional @OutputFile File secondaryOutputFile
                @TaskAction void doSomething() {
                    outputFile.text = inputFile.text
                    if (secondaryOutputFile != null) {
                        secondaryOutputFile.text = "secondary"
                    }
                }
            }
        """
        buildFile << """
            task customTask(type: CustomTask) {
                inputFile = file("input.txt")
                outputFile = file("build/output.txt")
                secondaryOutputFile = file("build/secondary.txt")
            }
        """

        when:
        withBuildCache().succeeds "customTask"
        then:
        nonSkippedTasks.contains ":customTask"
        file("build/output.txt").text == "data"
        file("build/secondary.txt").text == "secondary"
        file("build").listFiles().sort() as List == [file("build/output.txt"), file("build/secondary.txt")]

        when:
        cleanBuildDir()
        withBuildCache().succeeds "customTask"
        then:
        skippedTasks.contains ":customTask"
        file("build/output.txt").text == "data"
        file("build/secondary.txt").text == "secondary"
        file("build").listFiles().sort() as List == [file("build/output.txt"), file("build/secondary.txt")]

        when:
        cleanBuildDir()
        buildFile << """
            customTask.secondaryOutputFile = null
        """
        withBuildCache().succeeds "customTask"
        then:
        nonSkippedTasks.contains ":customTask"
        file("build/output.txt").text == "data"
        file("build").listFiles().sort() as List == [file("build/output.txt")]

        when:
        cleanBuildDir()
        withBuildCache().succeeds "customTask"
        then:
        skippedTasks.contains ":customTask"
        file("build/output.txt").text == "data"
        file("build").listFiles().sort() as List == [file("build/output.txt")]
    }

    def "plural output files are only restored when map keys match"() {
        configureCacheForBuildSrc()
        file("input.txt") << "data"
        file("buildSrc/src/main/groovy/CustomTask.groovy") << """
            import org.gradle.api.*
            import org.gradle.api.tasks.*

            @CacheableTask
            class CustomTask extends DefaultTask {
                @InputFile File inputFile
                @OutputFiles Map<String, File> outputFiles
                @TaskAction void doSomething() {
                    outputFiles.each { String key, File outputFile ->
                        outputFile.text = key
                    }
                }
            }
        """
        buildFile << """
            task customTask(type: CustomTask) {
                inputFile = file("input.txt")
                outputFiles = [
                    one: file("build/output-1.txt"),
                    two: file("build/output-2.txt")
                ]
            }
        """

        when:
        withBuildCache().succeeds "customTask"
        then:
        nonSkippedTasks.contains ":customTask"
        file("build/output-1.txt").text == "one"
        file("build/output-2.txt").text == "two"
        file("build").listFiles().sort() as List == [file("build/output-1.txt"), file("build/output-2.txt")]

        when:
        cleanBuildDir()
        buildFile << """
            customTask.outputFiles = [
                one: file("build/output-a.txt"),
                two: file("build/output-b.txt")
            ]
        """
        withBuildCache().succeeds "customTask"
        then:
        skippedTasks.contains ":customTask"
        file("build/output-a.txt").text == "one"
        file("build/output-b.txt").text == "two"
        file("build").listFiles().sort() as List == [file("build/output-a.txt"), file("build/output-b.txt")]

        when:
        cleanBuildDir()
        buildFile << """
            customTask.outputFiles = [
                first: file("build/output-a.txt"),
                second: file("build/output-b.txt")
            ]
        """
        withBuildCache().succeeds "customTask"
        then:
        nonSkippedTasks.contains ":customTask"
        file("build/output-a.txt").text == "first"
        file("build/output-b.txt").text == "second"
        file("build").listFiles().sort() as List == [file("build/output-a.txt"), file("build/output-b.txt")]
    }

    @Unroll
    def "missing #type output from runtime API is not cached"() {
        given:
        file("input.txt") << "data"
        buildFile << """
            task customTask {
                inputs.file "input.txt"
                outputs.file "build/output.txt" withPropertyName "output"
                outputs.$type "build/output/missing" withPropertyName "missing"
                outputs.cacheIf { true }
                doLast {
                    file("build").mkdirs()
                    file("build/output.txt").text = file("input.txt").text
                    delete("build/output/missing")
                }
            }
        """

        when:
        withBuildCache().succeeds "customTask"
        then:
        nonSkippedTasks.contains ":customTask"
        file("build/output.txt").text == "data"
        file("build/output").assertIsDir()
        file("build/output/missing").assertDoesNotExist()

        when:
        cleanBuildDir()
        withBuildCache().succeeds "customTask"
        then:
        skippedTasks.contains ":customTask"
        file("build/output.txt").text == "data"
        file("build/output").assertIsDir()
        file("build/output/missing").assertDoesNotExist()

        where:
        type << ["file", "dir"]
    }

    @Unroll
    def "missing #type from annotation API is not cached"() {
        given:
        file("input.txt") << "data"

        buildFile << """
            @CacheableTask
            class CustomTask extends DefaultTask {
                @InputFile File inputFile = project.file("input.txt")
                
                @${type} File missing = project.file("build/output/missing")
                @OutputFile File output = project.file("build/output.txt")
                
                @TaskAction void doSomething() {
                    output.text = inputFile.text
                    project.delete(missing)
                }
            }
            
            task customTask(type: CustomTask)
        """

        when:
        // creates the directory, but not the output file
        withBuildCache().succeeds "customTask"
        then:
        nonSkippedTasks.contains ":customTask"
        file("build/output.txt").text == "data"
        file("build/output").assertIsDir()
        file("build/output/missing").assertDoesNotExist()

        when:
        cleanBuildDir()
        withBuildCache().succeeds "customTask"
        then:
        skippedTasks.contains ":customTask"
        file("build/output.txt").text == "data"
        file("build/output").assertIsDir()
        file("build/output/missing").assertDoesNotExist()

        where:
        type << ["OutputFile", "OutputDirectory"]
    }

    def "empty output directory is cached properly"() {
        given:
        buildFile << """
            task customTask {
                outputs.dir "build/empty" withPropertyName "empty"
                outputs.cacheIf { true }
                doLast {
                    file("build/empty").mkdirs()
                }
            }
        """

        when:
        withBuildCache().succeeds "customTask"
        then:
        nonSkippedTasks.contains ":customTask"
        file("build/empty").assertIsEmptyDir()

        when:
        cleanBuildDir()
        withBuildCache().succeeds "customTask"
        then:
        skippedTasks.contains ":customTask"
        file("build/empty").assertIsEmptyDir()
    }

    @Unroll
    def "reports useful error when output #expected is expected but #actual is produced"() {
        given:
        file("input.txt") << "data"
        buildFile << """
            task customTask {
                inputs.file "input.txt"
                outputs.$expected "build/output" withPropertyName "output"
                outputs.cacheIf { true }
                doLast {
                    delete('build')
                    ${
                        actual == "file" ?
                            "mkdir('build'); file('build/output').text = file('input.txt').text"
                            : "mkdir('build/output'); file('build/output/output.txt').text = file('input.txt').text"
                    }
                }
            }
        """

        when:
        executer.withStackTraceChecksDisabled()
        withBuildCache().succeeds "customTask"
        then:
        def expectedMessage = message.replace("PATH", file("build/output").path)
        output.contains "Could not pack property 'output': $expectedMessage"

        where:
        expected | actual | message
        "file"   | "dir"  | "Expected 'PATH' to be a file"
        "dir"    | "file" | "Expected 'PATH' to be a directory"
    }

    def "task loaded with custom classloader is not cached"() {
        file("input.txt").text = "data"
        buildFile << """
            def CustomTask = new GroovyClassLoader(getClass().getClassLoader()).parseClass '''
                import org.gradle.api.*
                import org.gradle.api.tasks.*

                @CacheableTask
                class CustomTask extends DefaultTask {
                    @InputFile File input
                    @OutputFile File output
                    @TaskAction action() {
                        output.text = input.text
                    }
                }
            '''

            task customTask(type: CustomTask) {
                input = file("input.txt")
                output = file("build/output.txt")
            }
        """

        when:
        withBuildCache().succeeds "customTask", "--info"
        then:
        output.contains "Not caching task ':customTask' because no valid cache key was generated"
    }

    def "task with custom action loaded with custom classloader is not cached"() {
        file("input.txt").text = "data"
        buildFile << """
            import org.gradle.api.*
            import org.gradle.api.tasks.*

            @CacheableTask
            class CustomTask extends DefaultTask {
                @InputFile File input
                @OutputFile File output
                @TaskAction action() {
                    output.text = input.text
                }
            }

            def CustomTaskAction = new GroovyClassLoader(getClass().getClassLoader()).parseClass '''
                import org.gradle.api.*

                class CustomTaskAction implements Action<Task> {
                    static Action<Task> create() {
                        return new CustomTaskAction()
                    }

                    @Override
                    void execute(Task task) {
                    }
                }
            '''

            task customTask(type: CustomTask) {
                input = file("input.txt")
                output = file("build/output.txt")
                doFirst(CustomTaskAction.create())
            }
        """

        when:
        withBuildCache().succeeds "customTask", "--info"
        then:
        output.contains "Not caching task ':customTask' because no valid cache key was generated"
    }

    def "task stays up-to-date after loaded from cache"() {
        file("input.txt").text = "input"
        buildFile << defineProducerTask()

        withBuildCache().succeeds "producer"

        when:
        cleanBuildDir()
        withBuildCache().succeeds "producer"
        then:
        skippedTasks as List == [":producer"]

        when:
        withBuildCache().succeeds "producer", "--info"
        !output.contains("Caching disabled for task ':producer'")
        then:
        skippedTasks as List == [":producer"]
    }

    def "task can be cached after loaded from cache"() {
        file("input.txt").text = "input"
        buildFile << defineProducerTask()

        // Store in local cache
        withBuildCache().succeeds "producer"

        // Load from local cache
        cleanBuildDir()
        withBuildCache().succeeds "producer"

        // Store to local cache again
        when:
        cleanLocalBuildCache()
        withBuildCache().succeeds "producer", "--info", "--rerun-tasks"
        then:
        !output.contains("Caching disabled for task ':producer'")

        when:
        // Can load from local cache again
        cleanBuildDir()
        withBuildCache().succeeds "producer"
        then:
        skippedTasks as List == [":producer"]
    }

    def "re-ran task is not loaded from cache"() {
        file("input.txt").text = "input"
        buildFile << defineProducerTask()

        // Store in local cache
        withBuildCache().succeeds "producer"

        // Shouldn't load from cache
        when:
        withBuildCache().succeeds "producer", "--rerun-tasks"
        then:
        executed ":producer"
    }

    def "re-ran task is stored in cache"() {
        file("input.txt").text = "input"
        buildFile << defineProducerTask()

        // Store in local cache
        withBuildCache().succeeds "producer", "--rerun-tasks"

        when:
        withBuildCache().succeeds "producer"
        then:
        skipped ":producer"
    }

    def "downstream task stays cached when upstream task is loaded from cache"() {
        file("input.txt").text = "input"
        buildFile << defineProducerTask()
        buildFile << defineConsumerTask()

        withBuildCache().succeeds "consumer"

        when:
        cleanBuildDir()
        withBuildCache().succeeds "consumer"
        then:
        skippedTasks.sort() == [":consumer", ":producer"]
    }

    @Issue("https://github.com/gradle/gradle/issues/3043")
    def "URL-quoted characters in file names are handled properly"() {
        def weirdOutputPath = 'build/bad&dir/bad! Dezső %20.txt'
        def expectedOutput = file(weirdOutputPath)
        buildFile << """
            task weirdOutput {
                outputs.dir("build")
                outputs.cacheIf { true }
                doLast {
                    mkdir file('$weirdOutputPath').parentFile
                    file('$weirdOutputPath').text = "Data"
                }
            }
        """

        when:
        withBuildCache().succeeds "weirdOutput"
        then:
        executedAndNotSkipped ":weirdOutput"
        expectedOutput.file

        when:
        cleanBuildDir()
        withBuildCache().succeeds "weirdOutput"
        then:
        skipped ":weirdOutput"
        expectedOutput.file
    }

    @Unroll
    def "local state declared via #api API is destroyed when task is loaded from cache"() {
        def localStateFile = file("local-state.json")
        buildFile << defineTaskWithLocalState(useRuntimeApi)

        when:
        withBuildCache().succeeds "customTask"
        then:
        executedAndNotSkipped ":customTask"
        localStateFile.assertIsFile()

        when:
        cleanBuildDir()
        withBuildCache().succeeds "customTask"
        then:
        skipped ":customTask"
        localStateFile.assertDoesNotExist()

        where:
        useRuntimeApi << [true, false]
        api = useRuntimeApi ? "runtime" : "annotation"
    }

    @Unroll
    def "local state declared via #api API is not destroyed when task is not loaded from cache"() {
        def localStateFile = file("local-state.json")
        buildFile << defineTaskWithLocalState(useRuntimeApi)

        when:
        succeeds "customTask"
        then:
        executedAndNotSkipped ":customTask"
        localStateFile.assertIsFile()

        when:
        cleanBuildDir()
        succeeds "customTask", "-PassertLocalState"
        then:
        executedAndNotSkipped ":customTask"

        where:
        useRuntimeApi << [true, false]
        api = useRuntimeApi ? "runtime" : "annotation"
    }

    @Unroll
    def "null local state declared via #api API is supported"() {
        buildFile << defineTaskWithLocalState(useRuntimeApi, localStateFile)

        when:
        succeeds "customTask"
        then:
        executedAndNotSkipped ":customTask"

        where:
        useRuntimeApi | localStateFile
        true          | "{ null }"
        false         | "null"
        api = useRuntimeApi ? "runtime" : "annotation"
    }

    private static String defineProducerTask() {
        """
            import org.gradle.api.*
            import org.gradle.api.tasks.*

            @CacheableTask
            class ProducerTask extends DefaultTask {
                @InputFile File input
                @Optional @OutputFile nullFile
                @Optional @OutputDirectory nullDir
                @OutputFile File missingFile
                @OutputDirectory File missingDir
                @OutputFile File regularFile
                @OutputDirectory File emptyDir
                @OutputDirectory File singleFileInDir
                @OutputDirectory File manyFilesInDir
                @TaskAction action() {
                    project.delete(missingFile)
                    project.delete(missingDir)
                    regularFile.text = "regular file"
                    project.file("\$singleFileInDir/file.txt").text = "single file in dir"
                    project.file("\$manyFilesInDir/file-1.txt").text = "file #1 in dir"
                    project.file("\$manyFilesInDir/file-2.txt").text = "file #2 in dir"
                }
            }

            task producer(type: ProducerTask) {
                input = file("input.txt")
                missingFile = file("build/missing-file.txt")
                missingDir = file("build/missing-dir")
                regularFile = file("build/regular-file.txt")
                emptyDir = file("build/empty-dir")
                singleFileInDir = file("build/single-file-in-dir")
                manyFilesInDir = file("build/many-files-in-dir")
            }
        """
    }

    private static String defineConsumerTask() {
        """
            import org.gradle.api.*
            import org.gradle.api.tasks.*

            @CacheableTask
            class ConsumerTask extends DefaultTask {
                @InputFile File regularFile
                @InputDirectory File emptyDir
                @InputDirectory File singleFileInDir
                @InputDirectory File manyFilesInDir
                @OutputFile File output
                @TaskAction action() {
                    output.text = "output"
                }
            }

            task consumer(type: ConsumerTask) {
                dependsOn producer
                regularFile = producer.regularFile
                emptyDir = producer.emptyDir
                singleFileInDir = producer.singleFileInDir
                manyFilesInDir = producer.manyFilesInDir
                output = file("build/output.txt")
            }
        """
    }

    private TestFile cleanBuildDir() {
        file("build").assertIsDir().deleteDir()
    }

    void taskIsNotCached(String task) {
        withBuildCache().run task
        assert nonSkippedTasks.contains(task)
        cleanBuildDir()

        withBuildCache().run task
        assert nonSkippedTasks.contains(task)
    }

    void taskIsCached(String task) {
        withBuildCache().run task
        assert nonSkippedTasks.contains(task)
        cleanBuildDir()

        withBuildCache().run task
        assert skippedTasks.contains(task)
    }
}
