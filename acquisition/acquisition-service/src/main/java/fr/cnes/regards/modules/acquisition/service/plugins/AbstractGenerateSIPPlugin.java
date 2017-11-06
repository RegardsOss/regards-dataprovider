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

import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.builder.PDIBuilder;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.plugins.IGenerateSIPPlugin;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionException;
import fr.cnes.regards.modules.entities.client.IDatasetClient;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;
import fr.cnes.regards.modules.ingest.domain.builder.SIPCollectionBuilder;

/**
 * 
 * @author Christophe Mertz
 *
 */
public abstract class AbstractGenerateSIPPlugin implements IGenerateSIPPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGenerateSIPPlugin.class);

    public static final String META_PRODUCT_PARAM = "meta-produt";
    
    public static final String INGEST_PROCESSING_CHAIN_PARAM = "ingest-processing-chain";
    
    public static final String SESSION_PARAM = "sesssion-id";

    @Autowired
    private IDatasetClient datasetClient;

    SIPCollectionBuilder sipCollectionBuilder;

    @Override
    public SIPCollection runPlugin(List<AcquisitionFile> acqFiles, Optional<String> datasetIpId)
            throws ModuleException {
        String productName = acqFiles.get(0).getProduct().getProductName();

        LOGGER.info("Start SIP generation for product <{}>", productName);

        // Init the builder
        SIPBuilder sipBuilder = new SIPBuilder(productName);

        // Add all AcquisistionFile to the content information
        addDataObjectsToSip(sipBuilder, acqFiles);

        // Get the dataset name
        Optional<String> datasetName = Optional.empty();
        if (datasetIpId.isPresent()) {
            datasetName = Optional
                    .of(datasetClient.retrieveDataset(datasetIpId.get()).getBody().getContent().getLabel());
        }

        // Extracts the meta-attributes
        SortedMap<Integer, Attribute> mm = this.createMetadataPlugin(acqFiles, datasetName);

        addAttributesTopSip(sipBuilder, mm);

        // Add the SIP to the SIPCollection
        SIP aSip = sipBuilder.build();
        if (LOGGER.isDebugEnabled()) {
            Gson gson = new Gson();
            LOGGER.debug(gson.toJson(aSip));
        }
        this.sipCollectionBuilder.add(aSip);

        SIPCollection sipCollection = this.sipCollectionBuilder.build();

        // If a dataSet is defined, add a tag to the PreservationDescriptionInformation
        addDatasetTag(sipCollection, datasetIpId);

        LOGGER.info("End SIP generation for product <{}>", productName);

        return this.sipCollectionBuilder.build();
    }

    protected void addDatasetTag(SIPCollection sipCollection, Optional<String> datasetIpId) {
        // If a dataSet is defined, add a tag to the PreservationDescriptionInformation
        if (datasetIpId.isPresent()) {
            sipCollection.getFeatures().forEach(sip -> {
                PDIBuilder pdiBuilder = new PDIBuilder(sip.getProperties().getPdi());
                pdiBuilder.addTags(datasetIpId.get());
                sip.getProperties().setPdi(pdiBuilder.build());

                if (LOGGER.isDebugEnabled()) {
                    Gson gson = new Gson();
                    LOGGER.debug(gson.toJson(sip));
                }

            });
        }
    }

    protected abstract void addAttributesTopSip(SIPBuilder sipBuilder, SortedMap<Integer, Attribute> mapAttrs)
            throws AcquisitionException;

    protected abstract void addDataObjectsToSip(SIPBuilder sipBuilder, List<AcquisitionFile> acqFiles)
            throws AcquisitionException;

}