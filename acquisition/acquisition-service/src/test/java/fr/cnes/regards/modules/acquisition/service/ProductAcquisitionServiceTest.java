/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.acquisition.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.utils.plugins.PluginParameterTransformer;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMonitor;
import fr.cnes.regards.modules.acquisition.domain.chain.StorageMetadataProvider;
import fr.cnes.regards.modules.acquisition.service.job.AcquisitionJobPriority;
import fr.cnes.regards.modules.acquisition.service.job.ProductAcquisitionJob;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultFileValidation;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultProductPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultSIPGeneration;
import fr.cnes.regards.modules.acquisition.service.plugins.GlobDiskScanning;
import fr.cnes.regards.modules.acquisition.service.plugins.GlobDiskStreamScanning;
import fr.cnes.regards.modules.acquisition.service.session.SessionProductPropertyEnum;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionMonitoringEvent;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationOperator;
import fr.cnes.regards.modules.templates.service.ITemplateService;

/**
 * Test {@link AcquisitionProcessingService} for {@link Product} workflow
 *
 * @author Marc Sordi
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=acq_product" })
public class ProductAcquisitionServiceTest extends AbstractMultitenantServiceTest {

    @SpyBean
    INotificationClient notificationClient;

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductAcquisitionServiceTest.class);

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Autowired
    private IAcquisitionFileRepository acqFileRepository;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IProductService productService;

    @SuppressWarnings("unused")
    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private IAcquisitionFileService fileService;

    @Autowired
    IPluginConfigurationRepository pluginConfigurationRepository;

    @Autowired
    private ITemplateService templateService;

    @Autowired
    private IJobInfoService jobInfoService;

    @SpyBean
    private IPublisher publisher;

    @Before
    public void before() throws ModuleException {
        simulateApplicationReadyEvent();
        pluginConfigurationRepository.deleteAll();
        templateService.initDefaultTemplates();
        acqFileRepository.deleteAll();
        productRepository.deleteAll();
    }

    public AcquisitionProcessingChain createProcessingChain(Path searchDir) throws ModuleException {

        // Create a processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel("Product");
        processingChain.setActive(Boolean.TRUE);
        processingChain.setMode(AcquisitionProcessingChainMode.MANUAL);
        processingChain.setIngestChain("DefaultIngestChain");
        processingChain.setPeriodicity("0 * * * * *");
        processingChain.setCategories(Sets.newLinkedHashSet());

        // Create an acquisition file info
        AcquisitionFileInfo fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("A comment");
        fileInfo.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        fileInfo.setDataType(DataType.RAWDATA);

        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(GlobDiskScanning.FIELD_DIRS,
                                        PluginParameterTransformer.toJson(Arrays.asList(searchDir.toString()))));

        PluginConfiguration scanPlugin = PluginConfiguration.build(GlobDiskScanning.class, null, parameters);
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin");
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        // Validation
        PluginConfiguration validationPlugin = PluginConfiguration.build(DefaultFileValidation.class, null,
                                                                         new HashSet<IPluginParam>());
        validationPlugin.setIsActive(true);
        validationPlugin.setLabel("Validation plugin");
        processingChain.setValidationPluginConf(validationPlugin);

        // Product
        PluginConfiguration productPlugin = PluginConfiguration.build(DefaultProductPlugin.class, null,
                                                                      new HashSet<IPluginParam>());
        productPlugin.setIsActive(true);
        productPlugin.setLabel("Product plugin");
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginConfiguration.build(DefaultSIPGeneration.class, null,
                                                                     new HashSet<IPluginParam>());
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("SIP generation plugin");
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // SIP post processing
        // Not required

        List<StorageMetadataProvider> storages = new ArrayList<>();
        storages.add(StorageMetadataProvider.build("AWS", "/path/to/file", new HashSet<>()));
        storages.add(StorageMetadataProvider.build("HELLO", "/other/path/to/file", new HashSet<>()));
        processingChain.setStorages(storages);

        // Save processing chain
        processingChain = processingService.createChain(processingChain);

        // we need to set up a fake ProductAcquisitionJob to fill its attributes
        JobInfo jobInfo = new JobInfo(true);
        jobInfo.setPriority(AcquisitionJobPriority.PRODUCT_ACQUISITION_JOB_PRIORITY.getPriority());
        jobInfo.setParameters(new JobParameter(ProductAcquisitionJob.CHAIN_PARAMETER_ID, processingChain.getId()),
                              new JobParameter(ProductAcquisitionJob.CHAIN_PARAMETER_SESSION, "my funky session"));
        jobInfo.setClassName(ProductAcquisitionJob.class.getName());
        jobInfo.setOwner("user 1");
        // Create Job as pending to avoid Job manager to run it automaticly. This test run manualy the job function
        jobInfoService.createAsPending(jobInfo);

        processingChain.setLastProductAcquisitionJobInfo(jobInfo);

        return processingService.updateChain(processingChain);
    }

    //    @Test
    //    public void scanTest() throws ModuleException {
    //
    //        AcquisitionProcessingChain processingChain = createProcessingChain(Paths.get("src", "test", "resources", "data",
    //                                                                                     "plugins", "scan"));
    //        processingService.scanAndRegisterFiles(processingChain);
    //
    //        processingService.scanAndRegisterFiles(processingChain);
    //
    //        processingService.scanAndRegisterFiles(processingChain);
    //    }

    public AcquisitionProcessingChain createProcessingChainWithStream(Path searchDir) throws ModuleException {

        // Create a processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel("Product streamed");
        processingChain.setActive(Boolean.TRUE);
        processingChain.setMode(AcquisitionProcessingChainMode.MANUAL);
        processingChain.setIngestChain("DefaultIngestChain");
        processingChain.setCategories(Sets.newHashSet());

        // Create an acquisition file info
        AcquisitionFileInfo fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("A streamed comment");
        fileInfo.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        fileInfo.setDataType(DataType.RAWDATA);

        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(GlobDiskStreamScanning.FIELD_DIRS,
                                        PluginParameterTransformer.toJson(Arrays.asList(searchDir.toString()))));

        PluginConfiguration scanPlugin = PluginConfiguration.build(GlobDiskStreamScanning.class, null, parameters);
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan streamed plugin");
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        // Validation
        PluginConfiguration validationPlugin = PluginConfiguration.build(DefaultFileValidation.class, null,
                                                                         new HashSet<IPluginParam>());
        validationPlugin.setIsActive(true);
        validationPlugin.setLabel("Validation streamed plugin");
        processingChain.setValidationPluginConf(validationPlugin);

        // Product
        PluginConfiguration productPlugin = PluginConfiguration.build(DefaultProductPlugin.class, null,
                                                                      new HashSet<IPluginParam>());

        productPlugin.setIsActive(true);
        productPlugin.setLabel("Product streamed plugin");
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginConfiguration.build(DefaultSIPGeneration.class, null,
                                                                     new HashSet<IPluginParam>());

        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("SIP generation streamed plugin");
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // SIP post processing
        // Not required

        List<StorageMetadataProvider> storages = new ArrayList<>();
        storages.add(StorageMetadataProvider.build("AWS", "/path/to/file", new HashSet<>()));
        storages.add(StorageMetadataProvider.build("HELLO", "/other/path/to/file", new HashSet<>()));
        processingChain.setStorages(storages);

        // we need to set up a fake ProductAcquisitionJob to fill its attributes
        JobInfo jobInfo = new JobInfo(true);
        jobInfo.setPriority(AcquisitionJobPriority.PRODUCT_ACQUISITION_JOB_PRIORITY.getPriority());
        jobInfo.setParameters(new JobParameter(ProductAcquisitionJob.CHAIN_PARAMETER_ID, processingChain.getId()),
                              new JobParameter(ProductAcquisitionJob.CHAIN_PARAMETER_SESSION, "my funky session"));
        jobInfo.setClassName(ProductAcquisitionJob.class.getName());
        jobInfo.setOwner("user 1");
        jobInfoService.createAsPending(jobInfo);

        processingChain.setLastProductAcquisitionJobInfo(jobInfo);

        // Save processing chain
        return processingService.createChain(processingChain);
    }

    @Test
    public void acquisitionByStreamWorkflowTest() throws ModuleException {
        AcquisitionProcessingChain processingChain = createProcessingChainWithStream(Paths
                .get("src/test/resources/data/income/stream_test"));
        Mockito.reset(publisher);
        String session = "session1";
        processingService.scanAndRegisterFiles(processingChain, session);
        processingService.manageRegisteredFiles(processingChain, session);
        Assert.assertEquals("Invalid number of files registered", 5, acqFileRepository.findAll().size());
        Assert.assertEquals("Invalid number of products", 5, productRepository.findAll().size());
    }

    @Test
    public void acquisitionWorkflowTest() throws ModuleException, InterruptedException {

        AcquisitionProcessingChain processingChain = createProcessingChain(Paths.get("src", "test", "resources", "data",
                                                                                     "plugins", "scan"));
        AcquisitionFileInfo fileInfo = processingChain.getFileInfos().iterator().next();

        Mockito.reset(publisher);

        String session = "session1";
        processingService.scanAndRegisterFiles(processingChain, session);

        // Check registered files
        Page<AcquisitionFile> inProgressFiles = acqFileRepository
                .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.IN_PROGRESS, fileInfo, PageRequest.of(0, 1));
        Assert.assertTrue(inProgressFiles.getTotalElements() == 4);

        processingService.manageRegisteredFiles(processingChain, session);

        // Check registered files
        inProgressFiles = acqFileRepository
                .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.IN_PROGRESS,
                                                    processingChain.getFileInfos().iterator().next(),
                                                    PageRequest.of(0, 1));
        Assert.assertTrue(inProgressFiles.getTotalElements() == 0);

        Page<AcquisitionFile> validFiles = acqFileRepository
                .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.VALID, fileInfo, PageRequest.of(0, 1));
        Assert.assertTrue(validFiles.getTotalElements() == 0);

        Page<AcquisitionFile> acquiredFiles = acqFileRepository
                .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.ACQUIRED, fileInfo, PageRequest.of(0, 1));
        Assert.assertTrue(acquiredFiles.getTotalElements() == 4);

        // Find product to schedule
        long scheduled = productService.countByProcessingChainAndSipStateIn(processingChain,
                                                                            Arrays.asList(ProductSIPState.SCHEDULED));
        Assert.assertTrue(scheduled == 4);

        Assert.assertTrue(fileService.countByChain(processingChain) == 4);
        Assert.assertTrue(fileService.countByChainAndStateIn(processingChain,
                                                             Arrays.asList(AcquisitionFileState.ACQUIRED)) == 4);
        Assert.assertTrue(fileService.countByChainAndStateIn(processingChain,
                                                             Arrays.asList(AcquisitionFileState.ERROR)) == 0);

        Page<AcquisitionProcessingChainMonitor> monitor = processingService
                .buildAcquisitionProcessingChainSummaries(null, null, null, PageRequest.of(0, 10));
        Assert.assertTrue(!monitor.getContent().isEmpty());
        Assert.assertEquals(true, monitor.getContent().get(0).isActive());

        // Check product
        Assert.assertEquals(0, productService.countByProcessingChainAndSipStateIn(processingChain, Arrays
                .asList(ProductSIPState.GENERATION_ERROR, ProductSIPState.NOT_SCHEDULED_INVALID)));
        Assert.assertEquals(4, productService.countByChain(processingChain));
        Assert.assertEquals(4,
                            productService.countByProcessingChainAndSipStateIn(processingChain, Arrays
                                    .asList(ProductSIPState.NOT_SCHEDULED, ProductSIPState.SCHEDULED,
                                            ProductSIPState.SCHEDULED_INTERRUPTED)));

        // Check files
        Assert.assertEquals(0, fileService
                .countByChainAndStateIn(processingChain,
                                        Arrays.asList(AcquisitionFileState.ERROR, AcquisitionFileState.INVALID)));
        Assert.assertEquals(4, fileService.countByChain(processingChain));
        Assert.assertEquals(0, fileService
                .countByChainAndStateIn(processingChain,
                                        Arrays.asList(AcquisitionFileState.IN_PROGRESS, AcquisitionFileState.VALID)));

        // Wait for job ends
        Thread.sleep(5000);

        // Let's test SessionNotifier
        ArgumentCaptor<SessionMonitoringEvent> grantedInfo = ArgumentCaptor.forClass(SessionMonitoringEvent.class);
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(grantedInfo.capture());
        // Capture how many notif of each type have been sent
        Map<String, Integer> callByProperty = new HashMap<>();
        for (ISubscribable event : grantedInfo.getAllValues()) {
            // We ignore all others types of events
            if (event instanceof SessionMonitoringEvent) {
                SessionMonitoringEvent monitoringEvent = (SessionMonitoringEvent) event;
                String key = monitoringEvent.getProperty() + "_" + monitoringEvent.getOperator().toString();
                if (callByProperty.containsKey(key)) {
                    callByProperty.put(key, callByProperty.get(key) + 1);
                } else {
                    callByProperty.put(key, 1);
                }
            }
        }
        Integer incCompleted = callByProperty
                .get(SessionProductPropertyEnum.PROPERTY_COMPLETED.getValue() + "_" + SessionNotificationOperator.INC);
        Assert.assertNotNull(incCompleted);
        Assert.assertEquals(4, incCompleted.intValue());

        Integer decCompleted = callByProperty
                .get(SessionProductPropertyEnum.PROPERTY_COMPLETED.getValue() + "_" + SessionNotificationOperator.DEC);
        Assert.assertNotNull(decCompleted);
        Assert.assertEquals(4, decCompleted.intValue());

        Integer incGenerated = callByProperty
                .get(SessionProductPropertyEnum.PROPERTY_GENERATED.getValue() + "_" + SessionNotificationOperator.INC);
        Assert.assertNotNull(incGenerated);
        Assert.assertEquals(4, incGenerated.intValue());
    }
}
