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

package fr.cnes.regards.modules.acquisition.job.step;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanPlugin;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionFileService;
import fr.cnes.regards.modules.acquisition.service.IChainGenerationService;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionException;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionRuntimeException;

/**
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class AcquisitionScanStep extends AbstractStep implements IAcquisitionScanStep  {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionScanStep.class);

    @Autowired
    IPluginService pluginService;

    @Autowired
    IChainGenerationService chainGenerationService;

    @Autowired
    IAcquisitionFileRepository acquisitionFileRepository;

    @Autowired
    IAcquisitionFileService acquisitionFileService;

    private ChainGeneration chainGeneration;

    @Override
    public void proceedStep() throws AcquisitionRuntimeException {

        this.chainGeneration = process.getChainGeneration();

        // A plugin for the scan configuration is required
        if (this.chainGeneration.getScanAcquisitionPluginConf() == null) {
            throw new RuntimeException("The required IAcquisitionScanPlugin is missing for the ChainGeneration <"
                    + this.chainGeneration.getLabel() + ">");
        }

        // Lunch the scan plugin
        try {
            // build the plugin parameters
            PluginParametersFactory factory = PluginParametersFactory.build();
            for (Map.Entry<String, String> entry : this.chainGeneration.getScanAcquisitionParameter().entrySet()) {
                factory.addParameterDynamic(entry.getKey(), entry.getValue());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Add <" + entry.getKey() + "> parameter " + entry.getValue() + " : ");
                }
            }

            // get an instance of the plugin
            IAcquisitionScanPlugin scanPlugin = pluginService
                    .getPlugin(this.chainGeneration.getScanAcquisitionPluginConf(),
                               factory.getParameters().toArray(new PluginParameter[factory.getParameters().size()]));

            // launch the plugin to get the AcquisitionFile
            // c'est le plugin qui met la Date d'acquisition du fichier
            // c'est plugin qui calcule le checksum si c'est configuré dans la chaine   
            Set<AcquisitionFile> acquisitionFiles = scanPlugin.getAcquisitionFiles();

            synchronizedDatabase(acquisitionFiles);

        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    private void synchronizedDatabase(Set<AcquisitionFile> acquisitionFiles) {
        chainGenerationService.save(chainGeneration);

        for (AcquisitionFile af : acquisitionFiles) {
            List<AcquisitionFile> listAf = acquisitionFileService.findByMetaFile(af.getMetaFile());

            if (listAf.contains(af)) {
                // if the AcquisitionFile already exists in database
                // update his status and his date acquisition
                AcquisitionFile afExisting = listAf.get(listAf.indexOf(af));
                afExisting.setAcqDate(af.getAcqDate());
                afExisting.setStatus(AcquisitionFileStatus.IN_PROGRESS);
                acquisitionFileService.save(afExisting);
            } else {
                af.setStatus(AcquisitionFileStatus.IN_PROGRESS);
                acquisitionFileService.save(af);
            }
        }
    }

    @Override
    public void getResources() throws AcquisitionException {
    }

    @Override
    public void freeResources() throws AcquisitionException {
    }

    @Override
    public void stop() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void sleep() {
    }

    @Override
    public String getName() {
        return null;
    }

}
