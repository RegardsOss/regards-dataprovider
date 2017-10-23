/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

package fr.cnes.regards.modules.acquisition.service.plugins;

import java.io.File;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.gson.Gson;

import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IChainGenerationRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IScanDirectoryRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.ChainGenerationBuilder;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFileBuilder;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProductBuilder;
import fr.cnes.regards.modules.acquisition.domain.metadata.ScanDirectory;
import fr.cnes.regards.modules.acquisition.domain.metadata.ScanDirectoryBuilder;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.MetaProductDto;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.SetOfMetaFileDto;
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanDirectoryPlugin;
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanPlugin;
import fr.cnes.regards.modules.acquisition.service.AcquisitionFileServiceIT;
import fr.cnes.regards.modules.acquisition.service.IChainGenerationService;
import fr.cnes.regards.modules.acquisition.service.IMetaFileService;
import fr.cnes.regards.modules.acquisition.service.IMetaProductService;
import fr.cnes.regards.modules.acquisition.service.IScanDirectoryService;
import fr.cnes.regards.modules.acquisition.service.step.ChainGenerationServiceConfiguration;

/**
 * @author Christophe Mertz
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { ChainGenerationServiceConfiguration.class })
public class ScanDirectoryPluginIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionFileServiceIT.class);

    @Value("${regards.tenant}")
    private String tenant;

    private static final String CHAINE_LABEL = "the chain label";

    private static final String DATASET_NAME = "the dataset name";

    private static final String META_PRODUCT_NAME = "the meta product name";

    private static final String DEFAULT_USER = "John Doe";

    public static final String PATTERN_FILTER = "[A-Z]{4}_MESURE_TC_([0-9]{8}_[0-9]{6}).TXT";

    /*
     *  @see https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#MessageDigest
     */
    private static final String CHECKUM_ALGO = "SHA-256";

    @Autowired
    private IMetaProductService metaProductService;

    @Autowired
    private IMetaFileService metaFileService;

    @Autowired
    private IScanDirectoryService scandirService;

    @Autowired
    private IChainGenerationService chainService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IRabbitVirtualHostAdmin rabbitVhostAdmin;

    @Autowired
    private IAuthenticationResolver authenticationResolver;

    @Autowired
    private IMetaProductRepository metaProductRepository;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IScanDirectoryRepository scanDirectoryRepository;

    @Autowired
    private IChainGenerationRepository chainGenerationRepository;

    @Autowired
    private IAcquisitionFileRepository acquisitionFileRepository;

    @Autowired
    private IMetaFileRepository metaFileRepository;
    
    @Autowired
    private Gson gson;

    private ChainGeneration chain;

    @Before
    public void setUp() throws Exception {
        tenantResolver.forceTenant(tenant);

        cleanDb();

        initAmqp();

        initData();

        Mockito.when(authenticationResolver.getUser()).thenReturn(DEFAULT_USER);
    }

    public void initAmqp() {
        Assume.assumeTrue(rabbitVhostAdmin.brokerRunning());
        rabbitVhostAdmin.bind(tenantResolver.getTenant());
        rabbitVhostAdmin.unbind();

    }

    public void initData() {
        chain = chainService.save(ChainGenerationBuilder.build(CHAINE_LABEL).isActive().withDataSet(DATASET_NAME)
                .lastActivation(OffsetDateTime.now().minusHours(2)).get());
    }

    @After
    public void cleanDb() {
        scanDirectoryRepository.deleteAll();
        productRepository.deleteAll();
        acquisitionFileRepository.deleteAll();
        chainGenerationRepository.deleteAll();
        metaProductRepository.deleteAll();
        metaFileRepository.deleteAll();
    }

    @Test
    public void scanPluginTest() throws ModuleException {
        // Create a ScanDirectory
        URL dataPath = getClass().getResource("/data");
        ScanDirectory scanDir = scandirService.save(ScanDirectoryBuilder.build(dataPath.getPath())
                .withDateAcquisition(OffsetDateTime.now().minusDays(5)).get());

        MetaFile metaFile = metaFileService.save(MetaFileBuilder.build().withInvalidFolder("/var/regards/data/invalid")
                .withFileType(MediaType.APPLICATION_JSON_VALUE).withFilePattern(PATTERN_FILTER)
                .comment("test scan directory comment").isMandatory().addScanDirectory(scanDir).get());

        MetaProduct metaProduct = metaProductService.save(MetaProductBuilder.build(META_PRODUCT_NAME)
                .addMetaFile(metaFile).withChecksumAlgorithm(CHECKUM_ALGO).get());

        Set<MetaFile> metaFiles = new HashSet<>();
        metaFiles.add(metaFile);
        PluginParametersFactory factory = PluginParametersFactory.build();
        factory.addParameterDynamic(AbstractAcquisitionScanPlugin.META_PRODUCT_PARAM,
                                    gson.toJson(MetaProductDto.fromMetaProduct(metaProduct)));
        factory.addParameterDynamic(AbstractAcquisitionScanPlugin.META_FILE_PARAM,
                                    gson.toJson(SetOfMetaFileDto.fromSetOfMetaFile(metaFiles)));
        factory.addParameterDynamic(AbstractAcquisitionScanPlugin.CHAIN_GENERATION_PARAM, chain.getLabel());
        factory.addParameterDynamic(AbstractAcquisitionScanPlugin.LAST_ACQ_DATE_PARAM,
                                    gson.toJson(chain.getLastDateActivation()));

        IAcquisitionScanPlugin scanPlugin = pluginService
                .getPlugin(pluginService
                        .getPluginConfiguration("ScanDirectoryPlugin", IAcquisitionScanDirectoryPlugin.class)
                        .getId(), factory.getParameters().toArray(new PluginParameter[factory.getParameters().size()]));

        Assert.assertNotNull(scanPlugin);

        Set<AcquisitionFile> acqFiles = scanPlugin.getAcquisitionFiles();
        Assert.assertTrue(acqFiles != null && acqFiles.size() == 1);
        Assert.assertEquals(CHECKUM_ALGO, acqFiles.iterator().next().getChecksumAlgorithm());
        Assert.assertEquals("5b483e5260dc8f59bb1b57414b54ede8bc5344f1b2ef338e4463d9b4aa032f97",
                            acqFiles.iterator().next().getChecksum());

        Set<File> badFiles = scanPlugin.getBadFiles();
        Assert.assertTrue(badFiles != null && badFiles.size() == 3);
    }

    @Test
    public void scanPluginTestUnknowDirectory() throws ModuleException {
        ScanDirectory scanDir = scandirService.save(ScanDirectoryBuilder.build("/tmp/regards/data/unknown")
                .withDateAcquisition(OffsetDateTime.now().minusDays(5)).get());

        MetaFile metaFile = metaFileService.save(MetaFileBuilder.build().withInvalidFolder("/var/regards/data/invalid")
                .withFileType(MediaType.APPLICATION_JSON_VALUE).withFilePattern(PATTERN_FILTER)
                .comment("test scan directory comment").isMandatory().addScanDirectory(scanDir).get());

        Set<MetaFile> metaFiles = new HashSet<>();
        metaFiles.add(metaFile);
        PluginParametersFactory factory = PluginParametersFactory.build();
        factory.addParameterDynamic(AbstractAcquisitionScanPlugin.META_FILE_PARAM,
                                    new Gson().toJson(SetOfMetaFileDto.fromSetOfMetaFile(metaFiles)));
        factory.addParameterDynamic(AbstractAcquisitionScanPlugin.CHAIN_GENERATION_PARAM, chain.getLabel());

        IAcquisitionScanPlugin scanPlugin = pluginService
                .getPlugin(pluginService
                        .getPluginConfiguration("ScanDirectoryPlugin", IAcquisitionScanDirectoryPlugin.class)
                        .getId(), factory.getParameters().toArray(new PluginParameter[factory.getParameters().size()]));

        Assert.assertNotNull(scanPlugin);

        Set<AcquisitionFile> acqFiles = scanPlugin.getAcquisitionFiles();
        Assert.assertTrue(acqFiles != null && acqFiles.size() == 0);

        Set<File> badFiles = scanPlugin.getBadFiles();
        Assert.assertTrue(badFiles != null && badFiles.size() == 0);
    }

    @Test
    public void scanPluginTestWrongChecksumAlgo() throws ModuleException {
        // Create a ScanDirectory
        URL dataPath = getClass().getResource("/data");
        ScanDirectory scanDir = scandirService.save(ScanDirectoryBuilder.build(dataPath.getPath())
                .withDateAcquisition(OffsetDateTime.now().minusDays(5)).get());

        MetaFile metaFile = metaFileService.save(MetaFileBuilder.build().withInvalidFolder("/var/regards/data/invalid")
                .withFileType(MediaType.APPLICATION_JSON_VALUE).withFilePattern(PATTERN_FILTER)
                .comment("test scan directory comment").isMandatory().addScanDirectory(scanDir).get());

        MetaProduct metaProduct = metaProductService.save(MetaProductBuilder.build(META_PRODUCT_NAME)
                .addMetaFile(metaFile).withChecksumAlgorithm("UNKNOW").get());

        Set<MetaFile> metaFiles = new HashSet<>();
        metaFiles.add(metaFile);
        PluginParametersFactory factory = PluginParametersFactory.build();
        factory.addParameterDynamic(AbstractAcquisitionScanPlugin.META_PRODUCT_PARAM,
                                    new Gson().toJson(MetaProductDto.fromMetaProduct(metaProduct)));
        factory.addParameterDynamic(AbstractAcquisitionScanPlugin.META_FILE_PARAM,
                                    new Gson().toJson(SetOfMetaFileDto.fromSetOfMetaFile(metaFiles)));
        factory.addParameterDynamic(AbstractAcquisitionScanPlugin.CHAIN_GENERATION_PARAM, chain.getLabel());

        IAcquisitionScanPlugin scanPlugin = pluginService
                .getPlugin(pluginService
                        .getPluginConfiguration("ScanDirectoryPlugin", IAcquisitionScanDirectoryPlugin.class)
                        .getId(), factory.getParameters().toArray(new PluginParameter[factory.getParameters().size()]));

        Assert.assertNotNull(scanPlugin);

        Set<AcquisitionFile> acqFiles = scanPlugin.getAcquisitionFiles();
        Assert.assertTrue(acqFiles != null && acqFiles.size() == 1);
        Assert.assertNull(acqFiles.iterator().next().getChecksumAlgorithm());
        Assert.assertNull(acqFiles.iterator().next().getChecksum());
    }

}