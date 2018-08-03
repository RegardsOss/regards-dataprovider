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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.ConstrainedFields;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingService;

/**
 * {@link AcquisitionFile} REST API testing
 *
 * @author Marc Sordi
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=acquisition_it" })
@RegardsTransactional
public class AcquisitionFileControllerIT extends AbstractRegardsTransactionalIT {

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Before
    public void init() throws ModuleException {
        AcquisitionProcessingChain processingChain = AcquisitionTestUtils.getNewChain("Test");
        processingService.createChain(processingChain);

        Path basePath = Paths.get("src", "test", "resources", "input");
        for (int i = 1; i < 3; i++) {
            Path file1 = basePath.resolve("data_" + i + ".txt");
            processingService.registerFile(file1, processingChain.getFileInfos().get(0));
        }
    }

    @Test
    public void searchAllFiles() throws ModuleException {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        documentRequestParameters(requestBuilderCustomizer);

        requestBuilderCustomizer
                .addDocumentationSnippet(PayloadDocumentation.relaxedResponseFields(Attributes
                        .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TITLE).value("Acquisition file")),
                                                                                    documentAcquisitionFile()));

        performDefaultGet(AcquisitionFileController.TYPE_PATH, requestBuilderCustomizer, "Should retrieve files");
    }

    @Test
    public void searchFilesByState() throws ModuleException {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        documentRequestParameters(requestBuilderCustomizer);
        requestBuilderCustomizer.customizeRequestParam().param(AcquisitionFileController.REQUEST_PARAM_STATE,
                                                               AcquisitionFileState.IN_PROGRESS.toString());
        performDefaultGet(AcquisitionFileController.TYPE_PATH, requestBuilderCustomizer, "Should retrieve files");
    }

    private void documentRequestParameters(RequestBuilderCustomizer requestBuilderCustomizer) {

        ParameterDescriptor paramFilepath = RequestDocumentation
                .parameterWithName(AcquisitionFileController.REQUEST_PARAM_FILEPATH).optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .description("Entire file path filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional"));

        StringJoiner joiner = new StringJoiner(", ");
        for (AcquisitionFileState state : AcquisitionFileState.values()) {
            joiner.add(state.name());
        }
        ParameterDescriptor paramState = RequestDocumentation
                .parameterWithName(AcquisitionFileController.REQUEST_PARAM_STATE).optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .description("Acquisition file state filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                        .value("Optional. Multiple values allowed. Allowed values : " + joiner.toString()));

        ParameterDescriptor paramProductId = RequestDocumentation
                .parameterWithName(AcquisitionFileController.REQUEST_PARAM_PRODUCT_ID).optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_NUMBER_TYPE))
                .description("Product acquisition file(s) identifier filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional"));

        ParameterDescriptor paramChainId = RequestDocumentation
                .parameterWithName(AcquisitionFileController.REQUEST_PARAM_CHAIN_ID).optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_NUMBER_TYPE))
                .description("Acquisition chain identifier filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional"));

        ParameterDescriptor paramFrom = RequestDocumentation
                .parameterWithName(AcquisitionFileController.REQUEST_PARAM_FROM).optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .description("ISO Date time filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                        .value("Optional. Required format : yyyy-MM-dd'T'HH:mm:ss.SSSZ"));

        // Add request parameters documentation
        requestBuilderCustomizer.addDocumentationSnippet(RequestDocumentation
                .requestParameters(paramFilepath, paramState, paramProductId, paramChainId, paramFrom));
    }

    private List<FieldDescriptor> documentAcquisitionFile() {

        ConstrainedFields constrainedFields = new ConstrainedFields(AcquisitionFile.class);
        List<FieldDescriptor> fields = new ArrayList<>();

        String prefix = "content[].content.";

        fields.add(constrainedFields.withPath(prefix + "filePath", "filePath", "Local file path"));

        StringJoiner joiner = new StringJoiner(", ");
        for (AcquisitionFileState mode : AcquisitionFileState.values()) {
            joiner.add(mode.name());
        }
        fields.add(constrainedFields.withPath(prefix + "state", "state", "State",
                                              "Allowed values : " + joiner.toString()));

        fields.add(constrainedFields
                .withPath(prefix + "error", "error", "Error details when acquisition file state is in ERROR").optional()
                .type(JSON_STRING_TYPE));

        fields.add(constrainedFields.withPath(prefix + "acqDate", "acqDate", "ISO 8601 acquisition date"));

        fields.add(constrainedFields.withPath(prefix + "checksum", "checksum", "File checksum"));
        fields.add(constrainedFields.withPath(prefix + "checksumAlgorithm", "checksumAlgorithm", "Checksum algorithm"));

        return fields;
    }
}