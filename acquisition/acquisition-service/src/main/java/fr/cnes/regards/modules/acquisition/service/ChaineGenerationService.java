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
package fr.cnes.regards.modules.acquisition.service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.acquisition.builder.ProcessGenerationBuilder;
import fr.cnes.regards.modules.acquisition.dao.IChainGenerationRepository;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.ProcessGeneration;
import fr.cnes.regards.modules.acquisition.domain.job.ChainGenerationJobParameter;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.service.job.AcquisitionProductsJob;

/**
 * Manage global {@link ChainGeneration} life cycle
 * 
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class ChaineGenerationService implements IChainGenerationService {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ChaineGenerationService.class);

    /**
     * {@link ChainGeneration} repository
     */
    private final IChainGenerationRepository chainRepository;

    /**
     * {@link JobInfo} service
     */
    private final IJobInfoService jobInfoService;

    /**
     * {@link MetaProduct} service
     */
    private final IMetaProductService metaProductService;

    /**
     * {@link ProcessGeneration} service
     */
    private final IProcessGenerationService processService;

    /**
     * {@link Plugin} service
     */
    private final IPluginService pluginService;

    /**
     * Resolver to retrieve authentication information
     */
    @Autowired
    private IAuthenticationResolver authResolver;

    public ChaineGenerationService(ISubscriber subscriber, IChainGenerationRepository repository,
            IProcessGenerationService processService, IMetaProductService metaProductService,
            IJobInfoService jobInfoService, IPluginService pluginService) {
        super();
        this.chainRepository = repository;
        this.processService = processService;
        this.metaProductService = metaProductService;
        this.jobInfoService = jobInfoService;
        this.pluginService = pluginService;
    }

    @Override
    public ChainGeneration save(ChainGeneration chain) {
        return chainRepository.save(chain);
    }

    @Override
    public ChainGeneration create(ChainGeneration chain) throws ModuleException {
        Optional<ChainGeneration> chainGen = chainRepository.findOneByLabel(chain.getLabel());
        if (chainGen.isPresent()) {
            throw new EntityAlreadyExistsException(
                    String.format("%s for name %s aleady exists", ChainGeneration.class.getName(), chain.getLabel()));
        } else {
            return chainRepository.save(createOrUpdate(chain));
        }
    }

    private ChainGeneration createOrUpdate(ChainGeneration newChain, ChainGeneration... existingChain)
            throws ModuleException {

        if (existingChain != null) {
            // It's a modification
            // TODO CMZ
        }

        createOrUpdatePluginConfigurations(newChain);

        newChain.setMetaProduct(metaProductService.createOrUpdateMetaProduct(newChain.getMetaProduct()));

        return newChain;

    }

    /**
     * Creates or updates {@link PluginConfiguration} of each {@link Plugin} of the {@link ChainGeneration}
     * @param chain {@link ChainGeneration}
     * @throws ModuleException if error occurs!
     */
    private void createOrUpdatePluginConfigurations(ChainGeneration chain) throws ModuleException {
        // Save new plugins conf, and update existing ones if they changed
        if (chain.getCheckAcquisitionPluginConf() != null) {
            chain.setCheckAcquisitionPluginConf(createOrUpdatePluginConfiguration(chain
                    .getCheckAcquisitionPluginConf()));
        }
        if (chain.getGenerateSipPluginConf() != null) {
            chain.setGenerateSipPluginConf(createOrUpdatePluginConfiguration(chain.getGenerateSipPluginConf()));
        }
        if (chain.getPostProcessSipPluginConf() != null) {
            chain.setPostProcessSipPluginConf(createOrUpdatePluginConfiguration(chain.getPostProcessSipPluginConf()));
        }
    }

    /**
     * @param checkAcquisitionPluginConf
     * @return
     * @throws ModuleException 
     */
    private PluginConfiguration createOrUpdatePluginConfiguration(PluginConfiguration pluginConfiguration)
            throws ModuleException {
        if (pluginConfiguration.getId() == null) {
            return pluginService.savePluginConfiguration(pluginConfiguration);
        } else {
            PluginConfiguration existingConf = pluginService.getPluginConfiguration(pluginConfiguration.getId());
            if (!pluginConfiguration.equals(existingConf)) {
                return pluginService.savePluginConfiguration(pluginConfiguration);
            }
        }
        return pluginConfiguration;
    }

    @Override
    public ChainGeneration update(Long chainId, ChainGeneration chain) throws ModuleException {
        if (!chainId.equals(chain.getId())) {
            throw new EntityInconsistentIdentifierException(chainId, chain.getId(), chain.getClass());
        }
        if (!chainRepository.exists(chainId)) {
            throw new EntityNotFoundException(chainId, ChainGeneration.class);
        }

        ChainGeneration existingChain = chainRepository.findOne(chain.getId());

        return chainRepository.save(createOrUpdate(chain, existingChain));
    }

    @Override
    public Page<ChainGeneration> retrieveAll(Pageable page) {
        return chainRepository.findAll(page);
    }

    @Override
    public ChainGeneration retrieve(Long id) {
        return chainRepository.findOne(id);
    }

    @Override
    public ChainGeneration retrieveComplete(Long id) {
        ChainGeneration chain = this.retrieve(id);

        chain.setMetaProduct(metaProductService.retrieveComplete(chain.getMetaProduct().getId()));

        if (chain.getScanAcquisitionPluginConf() != null) {
            chain.setScanAcquisitionPluginConf(pluginService
                    .loadPluginConfiguration(chain.getScanAcquisitionPluginConf().getId()));
        }

        if (chain.getCheckAcquisitionPluginConf() != null) {
            chain.setCheckAcquisitionPluginConf(pluginService
                    .loadPluginConfiguration(chain.getCheckAcquisitionPluginConf().getId()));
        }

        if (chain.getGenerateSipPluginConf() != null) {
            chain.setGenerateSipPluginConf(pluginService
                    .loadPluginConfiguration(chain.getGenerateSipPluginConf().getId()));
        }

        if (chain.getPostProcessSipPluginConf() != null) {
            chain.setPostProcessSipPluginConf(pluginService
                    .loadPluginConfiguration(chain.getPostProcessSipPluginConf().getId()));
        }

        return chain;
    }

    @Override
    public void delete(Long id) {
        chainRepository.delete(id);
    }

    @Override
    public void delete(ChainGeneration chainGeneration) {
        chainRepository.delete(chainGeneration);
    }

    @Override
    public ChainGeneration findByMetaProduct(MetaProduct metaProduct) {
        return chainRepository.findByMetaProduct(metaProduct);
    }

    @Override
    public Set<ChainGeneration> findByActiveTrueAndRunningFalse() {
        return chainRepository.findByActiveTrueAndRunningFalse();
    }

    @Override
    public boolean run(Long id) {
        return run(this.retrieve(id));
    }

    @Override
    public boolean run(ChainGeneration chain) {
        // the ChainGeneration must be active
        if (!chain.isActive()) {
            LOGGER.warn("[{}] Unable to run a not active the chain generation", chain.getLabel());
            return false; // NOSONAR
        }

        // the ChainGeneration must not be already running
        if (chain.isRunning()) {
            LOGGER.warn("[{}] Unable to run an already running chain generation", chain.getLabel());
            return false; // NOSONAR
        }

        // the difference between the previous activation date and current time must be greater than the periodicity
        if ((chain.getLastDateActivation() != null)
                && chain.getLastDateActivation().plusSeconds(chain.getPeriodicity()).isAfter(OffsetDateTime.now())) {
            LOGGER.warn("[{}] Unable to run the chain generation : the last activation date is to close from now with the periodicity {}.",
                        chain.getLabel(), chain.getPeriodicity());
            return false; // NOSONAR
        }

        // the ChainGeneration is ready to be started 
        chain.setRunning(true);
        chain.setSession(chain.getLabel() + ":" + OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ":"
                + OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
        chainRepository.save(chain);

        // Create the ProcessGeneration
        processService.save(ProcessGenerationBuilder.build(chain.getSession()).withChain(chain)
                .withStartDate(OffsetDateTime.now()).get());

        LOGGER.info("[{}] a new session is created : {}", chain.getLabel(), chain.getSession());

        // Create a ScanJob
        JobInfo acquisition = new JobInfo();
        acquisition.setParameters(new ChainGenerationJobParameter(chain));
        acquisition.setClassName(AcquisitionProductsJob.class.getName());
        acquisition.setOwner(authResolver.getUser());
        acquisition.setPriority(50); //TODO CMZ priority ?

        acquisition = jobInfoService.createAsQueued(acquisition);

        return acquisition != null;
    }

}
