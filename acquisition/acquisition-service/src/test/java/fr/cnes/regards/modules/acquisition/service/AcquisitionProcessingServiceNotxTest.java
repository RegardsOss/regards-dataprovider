/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileInfoRepository;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.service.plugins.GlobDiskScanning;

/**
 * Test {@link AcquisitionProcessingService} for {@link AcquisitionProcessingChain} workflow
 *
 * @author Marc Sordi
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=acquisition2" })
public class AcquisitionProcessingServiceNotxTest extends AbstractMultitenantServiceTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionProcessingServiceNotxTest.class);

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Autowired
    private IAcquisitionFileInfoRepository fileInfoRepository;

    @Autowired
    private IPluginService pluginService;

    @Test
    public void registerWithDuplicates() throws ModuleException, IOException {

        // Create an acquisition file info
        AcquisitionFileInfo fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("A comment");
        fileInfo.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        fileInfo.setDataType(DataType.RAWDATA);

        Set<PluginParameter> param = PluginParametersFactory.build()
                .addParameter(GlobDiskScanning.FIELD_DIRS, new ArrayList<>()).getParameters();
        PluginConfiguration scanPlugin = PluginUtils.getPluginConfiguration(param, GlobDiskScanning.class);
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin");
        pluginService.savePluginConfiguration(scanPlugin);

        fileInfo.setScanPlugin(scanPlugin);

        fileInfoRepository.save(fileInfo);

        Path searchDir = Paths.get("src", "test", "resources", "data", "plugins", "scan");
        // Register file
        Path first = searchDir.resolve("CSSI_PRODUCT_01.md");
        Assert.assertTrue(processingService.registerFile(first, fileInfo, Optional.empty()));

        // Register same file with its lmd
        OffsetDateTime lmd = OffsetDateTime.ofInstant(Files.getLastModifiedTime(first).toInstant(), ZoneOffset.UTC);
        List<Path> filePaths = new ArrayList<>();
        filePaths.add(first);
        filePaths.add(searchDir.resolve("CSSI_PRODUCT_02.md"));
        filePaths.add(searchDir.resolve("CSSI_PRODUCT_03.md"));
        Assert.assertTrue(processingService.registerFiles(filePaths, fileInfo, Optional.of(lmd)) == 2);

    }
}