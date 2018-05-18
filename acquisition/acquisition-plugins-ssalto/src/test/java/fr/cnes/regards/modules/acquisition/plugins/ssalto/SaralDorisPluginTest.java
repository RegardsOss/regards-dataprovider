/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.plugins.ssalto;

import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.base.Optional;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.modules.acquisition.plugins.ISIPGenerationPluginWithMetadataToolbox;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.AbstractProductMetadataPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.SaralDoris10ProductMetadataPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.SaralProductMetadataPlugin;

/**
 * @author Christophe Mertz
 */
@ContextConfiguration(classes = { PluginsSsaltoTestsConfiguration.class })
@EnableAutoConfiguration
public class SaralDorisPluginTest extends AbstractProductMetadataPluginTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaralProductMetadataPlugin.class);

    @Autowired
    IPluginService pluginService;

    @Autowired
    IRuntimeTenantResolver runtimeTenantResoler;

    @Before
    public void start() {
        runtimeTenantResoler.forceTenant(DEFAULT_TENANT);
    }

    @Override
    public ISIPGenerationPluginWithMetadataToolbox buildPlugin(String datasetName) throws ModuleException {
        PluginConfiguration pluginConfiguration = this
                .getPluginConfiguration("SaralDoris10ProductMetadataPlugin", Optional.of(PluginParametersFactory.build()
                        .addParameter(AbstractProductMetadataPlugin.DATASET_SIP_ID, datasetName)
                        .addParameter(SaralDoris10ProductMetadataPlugin.ORF_FILE_PATH_PARAM,
                                      "src/test/resources/income/plugins/cycles/SRL_ORF_FIRST_CYCLES;src/test/resources/income/data/SARAL/ORF_HISTORIQUE/SRL_ORF_AXXCNE*")
                        .addParameter(SaralDoris10ProductMetadataPlugin.CYCLES_FILE_PATH_PARAM,
                                      "src/test/resources/income/data/SARAL/CYCLES/SARAL_CYCLES")
                        .getParameters()));

        return pluginService.getPlugin(pluginConfiguration.getId());
    }

    @Override
    public void initTestList() {
        addPluginTestDef("DA_TC_SARAL_DORIS10_COM", "SARAL/COMMERCIALES_10");
        addPluginTestDef("DA_TC_SARAL_DORIS10_FLAG", "SARAL/DOR10_INVALIDES");
        addPluginTestDef("DA_TC_SARAL_DORIS10_PUB", "SARAL/PUBLIQUES_10");
    }

    @Override
    public void initTestSoloList() {
        addPluginTestDef("DA_TC_SARAL_DORIS10_COM", "SARAL/COMMERCIALES_10",
                         "DSA_MEC_1PvS20081225_113759_20081225_235951_20081226_100205");
    }

    @Override
    public String getProjectProperties() {
        return "ssalto/domain/plugins/impl/saralplugin.properties";
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
