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

package fr.cnes.regards.modules.acquisition.service.job;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.framework.modules.jobs.domain.step.IProcessingStep;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.plugins.IScanPlugin;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingService;
import fr.cnes.regards.modules.acquisition.service.IProductService;
import fr.cnes.regards.modules.acquisition.service.job.step.AcquisitionCheckStep;
import fr.cnes.regards.modules.acquisition.service.job.step.AcquisitionScanStep;

/**
 * This class runs a set of step :<br>
 * <li>a step {@link AcquisitionScanStep} to scan and identify the {@link AcquisitionFile} to acquired
 * <li>a step {@link AcquisitionCheckStep} to check the {@link AcquisitionFile} and to determines the {@link Product}
 * associated<br>
 * And for each scanned {@link Product} not already send to Ingest microservice, and with its status equals to
 * {@link ProductState#COMPLETED} or {@link ProductState#FINISHED},
 * a new {@link JobInfo} of class {@link SIPGenerationJob} is create and queued.
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 *
 */
public class ProductAcquisitionJob extends AbstractJob<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductAcquisitionJob.class);

    public static final String CHAIN_PARAMETER_ID = "chain";

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private IProductService productService;

    @Autowired
    private IAcquisitionProcessingService acqProcessingService;

    @Autowired
    private IPluginService pluginService;

    /**
     * The current chain to work with!
     */
    private AcquisitionProcessingChain acqProcessingChain;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        Long acqProcessingChainId = getValue(parameters, CHAIN_PARAMETER_ID);
        try {
            acqProcessingChain = acqProcessingService.getChain(acqProcessingChainId);
        } catch (ModuleException e) {
            handleInvalidParameter(CHAIN_PARAMETER_ID, e.getMessage());
        }
    }

    @Override
    public void run() {

        // Get file informations
        Set<AcquisitionFileInfo> fileInfos = acqProcessingChain.getFileInfos();
        // Launch file scanning for each file information
        for (AcquisitionFileInfo info : fileInfos) {
            // Get plugin instance
            IScanPlugin scanPlugin = pluginService.getPlugin(info.getScanPlugin().getId());
            // Launch scanning
            // FIXME : may compute activation date per acquisition file
            List<Path> scannedFiles = scanPlugin.scan(acqProcessingChain.getLastActivationDate());
            // Initialize acquisition file
            // TODO
        }

        // Validate all files and get all related product name
        // TODO
        // Create missing product
        // TODO
        // Update product state
        // TODO

        LOGGER.info("[{}-{}] : starting acquisition job", acqProcessingChain.getLabel(),
                    acqProcessingChain.getSession());

        try {
            // Step 1 : required files scanning
            IProcessingStep<Void, Void> scanStep = new AcquisitionScanStep(this);
            beanFactory.autowireBean(scanStep);
            scanStep.execute(null);
            // Step 2 : optional files checking
            IProcessingStep<Void, Void> checkStep = new AcquisitionCheckStep(this);
            beanFactory.autowireBean(checkStep);
            checkStep.execute(null);

            // For each complete product, creates and schedules a job to generate SIP
            Set<Product> products = productService.findChainProductsToSchedule(acqProcessingChain);
            for (Product p : products) {
                productService.scheduleProductSIPGeneration(p, acqProcessingChain);
            }

            // Job is terminated ... release processing chain
            acqProcessingChain.setRunning(false);
            acqProcessChainService.createOrUpdate(acqProcessingChain);

            LOGGER.info("[{}-{}] : {} jobs for SIP generation queued", acqProcessingChain.getLabel(),
                        acqProcessingChain.getSession(), products.size());

        } catch (ModuleException pse) {
            LOGGER.error("Business error", pse);
            throw new JobRuntimeException(pse);
        }
    }

    public AcquisitionProcessingChain getAcqProcessingChain() {
        return acqProcessingChain;
    }
}
