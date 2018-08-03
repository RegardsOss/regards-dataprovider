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
package fr.cnes.regards.modules.acquisition.rest;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultFileValidation;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultProductPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultSIPGeneration;
import fr.cnes.regards.modules.acquisition.service.plugins.GlobDiskScanning;

/**
 * Utils for product acquisition testing
 *
 * @author Marc Sordi
 *
 */
public class AcquisitionTestUtils {

    public static AcquisitionProcessingChain getNewChain(String labelPrefix) {

        // Create a processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel("Processing chain 1");
        processingChain.setActive(Boolean.TRUE);
        processingChain.setMode(AcquisitionProcessingChainMode.MANUAL);
        processingChain.setIngestChain("DefaultIngestChain");

        // Create an acquisition file info
        AcquisitionFileInfo fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("A comment");
        fileInfo.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        fileInfo.setDataType(DataType.RAWDATA);

        List<PluginParameter> param = PluginParametersFactory.build()
                .addParameter(GlobDiskScanning.FIELD_DIRS, new ArrayList<>()).getParameters();
        PluginConfiguration scanPlugin = PluginUtils.getPluginConfiguration(param, GlobDiskScanning.class,
                                                                            Lists.newArrayList());
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel(labelPrefix + " : " + "Scan plugin");
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        // Validation
        PluginConfiguration validationPlugin = PluginUtils
                .getPluginConfiguration(Lists.newArrayList(), DefaultFileValidation.class, Lists.newArrayList());
        validationPlugin.setIsActive(true);
        validationPlugin.setLabel(labelPrefix + " : " + "Validation plugin");
        processingChain.setValidationPluginConf(validationPlugin);

        // Product
        PluginConfiguration productPlugin = PluginUtils
                .getPluginConfiguration(Lists.newArrayList(), DefaultProductPlugin.class, Lists.newArrayList());
        productPlugin.setIsActive(true);
        productPlugin.setLabel(labelPrefix + " : " + "Product plugin");
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginUtils
                .getPluginConfiguration(Lists.newArrayList(), DefaultSIPGeneration.class, Lists.newArrayList());
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel(labelPrefix + " : " + "SIP generation plugin");
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // SIP post processing
        // Not required

        return processingChain;
    }

}