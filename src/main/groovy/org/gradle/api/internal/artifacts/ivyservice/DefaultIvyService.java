/*
 * Copyright 2007-2008 the original author or authors.
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
package org.gradle.api.internal.artifacts.ivyservice;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Module;
import org.gradle.api.artifacts.PublishInstruction;
import org.gradle.api.internal.artifacts.IvyService;
import org.gradle.api.internal.artifacts.configurations.Configurations;
import org.gradle.api.internal.artifacts.configurations.DependencyMetaDataProvider;
import org.gradle.api.internal.artifacts.ivyservice.moduleconverter.DefaultModuleDescriptorConverter;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Hans Dockter
 */
public class DefaultIvyService implements IvyService {
    private SettingsConverter settingsConverter = new DefaultSettingsConverter();
    private ModuleDescriptorConverter moduleDescriptorConverter = new DefaultModuleDescriptorConverter();
    private IvyFactory ivyFactory = new DefaultIvyFactory();
    private IvyDependencyResolver dependencyResolver = new DefaultIvyDependencyResolver(new Report2Classpath());
    private IvyDependencyPublisher dependencyPublisher = new DefaultIvyDependencyPublisher(new DefaultPublishOptionsFactory());
    private final DependencyMetaDataProvider metaDataProvider;

    public DefaultIvyService(DependencyMetaDataProvider metaDataProvider) {
        this.metaDataProvider = metaDataProvider;
    }

    private Ivy ivyForResolve(List<DependencyResolver> dependencyResolvers, File cacheParentDir,
                   Map<String, ModuleDescriptor> clientModuleRegistry) {
        return ivyFactory.createIvy(
                settingsConverter.convertForResolve(
                        dependencyResolvers,
                        cacheParentDir,
                        metaDataProvider.getInternalRepository(),
                        clientModuleRegistry
                )
        );
    }

    private Ivy ivyForPublish(List<DependencyResolver> publishResolvers, File cacheParentDir) {
        return ivyFactory.createIvy(
                settingsConverter.convertForPublish(
                        publishResolvers,
                        cacheParentDir,
                        metaDataProvider.getInternalRepository()
                )
        );
    }

    public SettingsConverter getSettingsConverter() {
        return settingsConverter;
    }

    public ModuleDescriptorConverter getModuleDescriptorConverter() {
        return moduleDescriptorConverter;
    }

    public IvyFactory getIvyFactory() {
        return ivyFactory;
    }

    public DependencyMetaDataProvider getMetaDataProvider() {
        return metaDataProvider;
    }

    public IvyDependencyResolver getDependencyResolver() {
        return dependencyResolver;
    }

    public IvyDependencyPublisher getDependencyPublisher() {
        return dependencyPublisher;
    }

    public Set<File> resolve(Configuration configuration) {
        ResolveReport resolveReport = resolveAsReportInternal(configuration, true, metaDataProvider.getModule(), metaDataProvider.getGradleUserHomeDir(), metaDataProvider.getClientModuleRegistry());
        return dependencyResolver.resolveFromReport(configuration, resolveReport);
    }

    public Set<File> resolveFromReport(Configuration configuration, ResolveReport resolveReport) {
        return dependencyResolver.resolveFromReport(configuration, resolveReport);
    }

    public ResolveReport resolveAsReport(Configuration configuration) {
        return resolveAsReportInternal(configuration, false, metaDataProvider.getModule(), metaDataProvider.getGradleUserHomeDir(), metaDataProvider.getClientModuleRegistry());
    }

    private ResolveReport resolveAsReportInternal(Configuration configuration, boolean failOnError,
                                                  Module module, File cacheParentDir, Map<String, ModuleDescriptor> clientModuleRegistry) {
        Ivy ivy = ivyForResolve(configuration.getDependencyResolvers(),
                cacheParentDir,
                clientModuleRegistry);
        ModuleDescriptor moduleDescriptor = moduleDescriptorConverter.convertForResolve(configuration,
                module, clientModuleRegistry, ivy.getSettings());
        return dependencyResolver.resolveAsReport(configuration, ivy, moduleDescriptor, failOnError);
    }

    public void publish(Set<Configuration> configurationsToPublish, PublishInstruction publishInstruction,
                        List<DependencyResolver> publishResolvers) {
        assert configurationsToPublish.size() > 0;
        Ivy ivy = ivyForPublish(publishResolvers, metaDataProvider.getGradleUserHomeDir());
        Set<String> confs = Configurations.getNames(configurationsToPublish, false);
        dependencyPublisher.publish(
                confs,
                publishInstruction,
                publishResolvers,
                moduleDescriptorConverter.convertForPublish(configurationsToPublish, publishInstruction.isUploadDescriptor(),
                        metaDataProvider.getModule(), ivy.getSettings()),
                ivy.getPublishEngine());
    }

    public void setSettingsConverter(SettingsConverter settingsConverter) {
        this.settingsConverter = settingsConverter;
    }

    public void setModuleDescriptorConverter(ModuleDescriptorConverter moduleDescriptorConverter) {
        this.moduleDescriptorConverter = moduleDescriptorConverter;
    }

    public void setIvyFactory(IvyFactory ivyFactory) {
        this.ivyFactory = ivyFactory;
    }

    public void setDependencyResolver(IvyDependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
    }

    public void setDependencyPublisher(IvyDependencyPublisher dependencyPublisher) {
        this.dependencyPublisher = dependencyPublisher;
    }
}
